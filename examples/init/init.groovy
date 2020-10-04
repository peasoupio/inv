repo {
    name "main" // The name 'main' is mandatory.

    path "${env.TEMP ?: '/tmp'}/inv/init" // Change for your need
    src "https://github.com/peasoupio/inv.git"

    hooks {

        // Do the actual source code extraction (or clone in "git" terms).
        init """
git clone -b release/0.6-beta ${src} .

mkdir ./repos
cp examples/composer/repos/* ./repos
"""
        // Pull the latest changes.
        pull """
git reset --hard
git pull

cp examples/composer/repos/* ./repos
"""
        // Returns the current local repository branch as a version.
        version """
git rev-parse --abbrev-ref HEAD
"""
    }
}