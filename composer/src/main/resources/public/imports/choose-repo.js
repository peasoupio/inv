Vue.component('choose-repo', {
    template: `
<div>

    <div v-if="repos.total == undefined || repos.total == 0">
        No repo are available yet...
    </div>
    <div v-else>
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="stageAll()"class="button breath">
                    Stage all ({{repos.total - repos.staged}})
                </button>
            </div>
            <div class="field">
                <button @click="unstageAll()"class="button">
                    Unstage all ({{repos.staged}})
                </button>
            </div>
        </div>

        <table class="table is-striped is-narrow is-hoverable is-fullwidth" >
            <thead>
            <tr class="field">
                <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchRepo(true)"></th>
                <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchRepo(true)"></th>
                <th style="width: 10%">Options</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="repo in filter()">
                <td>{{repo.name}}</td>
                <td>
                    <a :href="repo.descriptor.src" v-if="repo.descriptor.src">{{repo.descriptor.src}}</a>
                    <span v-else><strong>undefined</strong></span>
                </td>
                <td v-if="!repo.staged"><button class="button is-link" @click.stop="stage(repo)" :disabled="repo.selected">Stage</button></td>
                <td v-if="repo.staged"><button class="button is-warning" @click.stop="unstage(repo)">Unstage</button></td>
            </tr>
            </tbody>
        </table>

        <pagination v-model="paginationSettings" />
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            repos: {},
            filters: {
                from: 0,
                step: 20,
                name: '',
                src: '',
                entry: ''
            }
        }
    },
    computed: {
        paginationSettings: {
            get() {
                var vm = this
                return {
                    refresh: function(from) {
                        vm.filters.from = from
                        vm.searchRepo()
                    },
                    from: vm.filters.from,
                    step: vm.filters.step,
                    total: vm.repos.count
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.repos.descriptors.forEach(function(repo) {
                filtered.push(repo)
            })

            return filtered.sort(compareValues('name'))
        },
        searchRepo: function(fromFilter) {
            var vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.repos.search, vm.filters).then(response => {
                vm.repos = response.data
            })
        },
        stage: function(repo) {
            var vm = this

            axios.post(repo.links.stage).then(response => {
                repo.staged = true
                vm.repos.staged++
            })
        },
        unstage: function(repo) {
            var vm = this

            axios.post(repo.links.unstage).then(response => {
                repo.staged = false
                vm.repos.staged--
            })
        },
        stageAll: function() {
            var vm = this

            axios.post(vm.value.api.links.repos.stageAll).then(response => {
                vm.searchRepo()
            })
        },
        unstageAll: function() {
            var vm = this

            axios.post(vm.value.api.links.repos.unstageAll).then(response => {
                vm.searchRepo()
            })
        }
    },
    created: function() {
        this.searchRepo()
    }
})