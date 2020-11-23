repo {
    name "my-repo"

    ask {
        parameter "myparam", "A parameter"
    }

    hooks {
        init """
echo ${myparam}
"""
    }
}