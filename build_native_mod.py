#!/usr/bin/env python3
import os
import glob
import subprocess
import zipfile
import hashlib
import shutil
import sys

def find_jdk_tool(tool_name):
    """Finds a JDK tool like javac or java prioritizing Java 25 epsilon for 26.3 compatibility."""
    epsilon = os.path.expandvars('%USERPROFILE%/AppData/Local/Packages/Microsoft.4297127D64EC6_8wekyb3d8bbwe/LocalCache/Local/runtime/java-runtime-epsilon/windows-x64/java-runtime-epsilon/bin/' + tool_name + '.exe')
    if os.path.exists(epsilon):
        return epsilon

    found = shutil.which(tool_name)
    if found:
        return found

    candidates = [
        os.path.expandvars('%JAVA_HOME%/bin/' + tool_name + '.exe'),
        'C:/Program Files/Microsoft/jdk-21.0.12.1-hotspot/bin/' + tool_name + '.exe'
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    return tool_name

def build():
    root = os.path.abspath(os.path.dirname(__file__))
    os.chdir(root)

    print("=================================================")
    print("      SmartCrafter Pro - Native Mod Builder      ")
    print("=================================================")

    javac = find_jdk_tool('javac')
    java = find_jdk_tool('java')
    print(f"Using javac: {javac}")
    print(f"Using java:  {java}")

    mc_dir = os.path.expandvars('%APPDATA%/.minecraft')
    libs_dir = os.path.join(mc_dir, 'libraries')
    mods_dir = os.path.join(mc_dir, 'mods')
    version_jar = os.path.join(mc_dir, 'versions', '26.3', '26.3.jar')

    os.makedirs('tmp_build', exist_ok=True)

    # 1. Prepare Compile Classpath
    cached_cp = os.path.join("D:/GEMINI AGY/workspaces/litematica-printer-pro/tmp_build/cp.txt")
    if os.path.exists(cached_cp):
        with open(cached_cp, 'r', encoding='utf-8') as f:
            cp_content = f.read()
        with open('tmp_build/cp.txt', 'w', encoding='utf-8') as f:
            f.write(cp_content)
    else:
        cp_jars = [version_jar]
        for f in glob.glob(os.path.join(libs_dir, '**', '*.jar'), recursive=True):
            cp_jars.append(f)
        for f in glob.glob(os.path.join(mc_dir, '.fabric', 'processedMods', '*.jar')):
            cp_jars.append(f)
        with open('tmp_build/cp.txt', 'w', encoding='utf-8') as f:
            f.write('-cp\n' + os.pathsep.join(cp_jars) + '\n')

    # 2. Collect Main Sources
    main_sources = []
    for r, d, files in os.walk('src/main/java'):
        for file in files:
            if file.endswith('.java'):
                main_sources.append(os.path.join(r, file).replace('\\', '/'))

    with open('tmp_build/sources.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(main_sources) + '\n')

    out_classes = os.path.join(root, 'build', 'classes', 'java', 'main')
    os.makedirs(out_classes, exist_ok=True)

    print(f"\n[1/4] Compiling {len(main_sources)} main Java sources...")
    cmd = [
        javac,
        '@tmp_build/cp.txt',
        '@tmp_build/sources.txt',
        '-d', out_classes,
        '-encoding', 'UTF-8',
        '--release', '21'
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print("Compilation FAILED:\n" + res.stderr)
        return False
    print("Main compilation SUCCESSFUL!")

    # 3. Compile and Run Unit Tests
    print("\n[2/4] Compiling and running unit tests...")
    gradle_cache = os.path.expandvars('%USERPROFILE%/.gradle/caches/modules-2/files-2.1')
    junit_jars = []
    for f in glob.glob(os.path.join(gradle_cache, '**', '*.jar'), recursive=True):
        basename = os.path.basename(f).lower()
        if any(k in basename for k in ['junit', 'opentest4j', 'apiguardian']):
            junit_jars.append(f)

    test_sources = []
    for r, d, files in os.walk('src/test/java'):
        for file in files:
            if file.endswith('.java'):
                test_sources.append(os.path.join(r, file).replace('\\', '/'))

    out_test_classes = os.path.join(root, 'build', 'classes', 'java', 'test')
    os.makedirs(out_test_classes, exist_ok=True)

    test_cp = [out_classes.replace('\\', '/')] + [j.replace('\\', '/') for j in junit_jars]
    # Also add Minecraft / fabric jars
    with open('tmp_build/cp.txt', 'r', encoding='utf-8') as f:
        lines = f.read().splitlines()
        if len(lines) > 1:
            for p in lines[1].split(os.pathsep):
                if p: test_cp.append(p.replace('\\', '/'))

    with open('tmp_build/test_cp.txt', 'w', encoding='utf-8') as f:
        f.write('-cp\n"' + os.pathsep.join(test_cp) + '"\n')

    with open('tmp_build/test_sources.txt', 'w', encoding='utf-8') as f:
        f.write('\n'.join(test_sources) + '\n')

    cmd_test_compile = [
        javac,
        '@tmp_build/test_cp.txt',
        '@tmp_build/test_sources.txt',
        '-d', out_test_classes,
        '-encoding', 'UTF-8',
        '--release', '21'
    ]
    res_test_compile = subprocess.run(cmd_test_compile, capture_output=True, text=True)
    if res_test_compile.returncode != 0:
        print("Test compilation FAILED:\n" + res_test_compile.stderr)
        return False

    # Execute Standalone Test Runner
    run_cp = [out_test_classes.replace('\\', '/'), out_classes.replace('\\', '/')] + [j.replace('\\', '/') for j in junit_jars]
    with open('tmp_build/run_cp.txt', 'w', encoding='utf-8') as f:
        f.write('-cp\n"' + os.pathsep.join(run_cp) + '"\n')

    cmd_test_run = [
        java,
        '@tmp_build/run_cp.txt',
        'com.smartcrafter.test.StandaloneTestRunner'
    ]
    res_test_run = subprocess.run(cmd_test_run, capture_output=True, text=True)
    print(res_test_run.stdout)
    if res_test_run.returncode != 0:
        print("Tests FAILED:\n" + res_test_run.stderr)
        return False

    # 4. Package Artifacts
    print("[3/4] Packaging SmartCrafter Pro JAR...")
    dist_dir = os.path.join(root, 'build', 'libs')
    os.makedirs(dist_dir, exist_ok=True)
    jar_path = os.path.join(dist_dir, 'smart-crafter-pro-1.0.0.jar')

    with zipfile.ZipFile(jar_path, 'w', zipfile.ZIP_DEFLATED) as zf:
        # Class files
        for r, d, files in os.walk(out_classes):
            for file in files:
                full_path = os.path.join(r, file)
                rel_path = os.path.relpath(full_path, out_classes).replace('\\', '/')
                zf.write(full_path, rel_path)

        # Resources
        res_dir = os.path.join(root, 'src', 'main', 'resources')
        for r, d, files in os.walk(res_dir):
            for file in files:
                full_path = os.path.join(r, file)
                rel_path = os.path.relpath(full_path, res_dir).replace('\\', '/')
                zf.write(full_path, rel_path)

        # LICENSE
        license_path = os.path.join(root, 'LICENSE')
        if os.path.exists(license_path):
            zf.write(license_path, 'LICENSE_smart-crafter-pro')

    # Copy to Minecraft mods folder if exists
    if os.path.exists(mods_dir):
        shutil.copyfile(jar_path, os.path.join(mods_dir, 'smart-crafter-pro-1.0.0.jar'))
        print(f"Installed to Minecraft mods folder: {mods_dir}")

    # 5. Checksums
    print("[4/4] Generating SHA256 checksum...")
    sha256 = hashlib.sha256(open(jar_path, 'rb').read()).hexdigest()
    checksum_file = os.path.join(dist_dir, 'CHECKSUMS.txt')
    with open(checksum_file, 'w', encoding='utf-8') as f:
        f.write(f"{sha256}  smart-crafter-pro-1.0.0.jar\n")

    print("\n=================================================")
    print("             BUILD COMPLETED SUCCESSFULLY        ")
    print(f"JAR:      {jar_path}")
    print(f"SHA-256:  {sha256}")
    print("=================================================")
    return True

if __name__ == '__main__':
    success = build()
    sys.exit(0 if success else 1)
