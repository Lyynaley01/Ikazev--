# Termux: from zero

Install Termux from a trusted source such as F-Droid or the official Termux GitHub releases. Then:

```sh
pkg update && pkg upgrade -y
pkg install -y git openssh
mkdir -p ~/projects
cd ~/projects
```

After creating the GitHub repository `Ikazev-𝙰𝚊𝙰`, upload the project ZIP contents so `settings.gradle.kts` is at repository root. If the ZIP is in Android Downloads, use your file manager to extract it, then copy the extracted project folder into `~/projects/`.

Example (adjust the source folder to your actual extracted location):
```sh
cp -r /sdcard/Download/Ikazev-AaA ~/projects/
cd ~/projects/Ikazev-AaA
git init
git branch -M main
git add .
git commit -m "Initial AI Answer Bubble Android project"
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/Ikazev-𝙰𝚊𝙰.git
git push -u origin main
```

If GitHub rejects the Unicode repository name, create `Ikazev-AaA` and update the remote URL accordingly. Authenticate using Git Credential Manager where available or a GitHub token entered securely when Git prompts; never hard-code tokens in commands or source files.

This workflow compiles on GitHub's hosted Linux runner, so Termux does not need to install Android SDK/Gradle locally. After each push, visit the repository's Actions tab, open the latest successful run, and download the APK artifact.

## Local editing
Use `nano app/src/main/java/com/ikazev/answerbubble/...` or your preferred editor. Commit and push changes to trigger a new build.
