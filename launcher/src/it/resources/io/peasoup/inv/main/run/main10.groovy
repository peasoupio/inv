package io.peasoup.inv.main.run

get("https://raw.githubusercontent.com/peasoupio/inv-public-repo/master/tools/maven/repo.yml")

inv {
    name "main1"
    require { Maven } using {
        defaults false
    }
}

