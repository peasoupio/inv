scm {
    name "main" // Atleast one scm named 'main' is required.

    path "${env.TEMP ?: '/tmp'}/inv/init" // Change for your need
    src "https://github.com/peasoupio/inv.git"

    hooks {
        init """
# Remove '#' to use example composer workspace 
# git clone -b release/0.5-beta ${src} ./gitExtract
# mv ./gitExtract/src/main/example/composer/* ./
# rm -rf ./gitExtract
"""
    }
}