package io.peasoup.inv.main.repo.list

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