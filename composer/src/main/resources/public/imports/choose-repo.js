Vue.component('choose-repo', {
    template: `
<div>

    <div v-if="repos.total == undefined || repos.total == 0">
        No repo are available yet...
    </div>
    <div v-else>
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="toggleSearchOptions('staged')" v-bind:class="{ 'is-link': filters.staged}" class="button breath">
                    Show only staged ({{repos.staged}}/{{repos.count}})
                </button>
                <button @click="toggleSearchOptions('required')" v-bind:class="{ 'is-link': filters.required}" class="button breath">
                    Show all required ({{repos.required}}/{{repos.count}})
                </button>
            </div>
            <div class="field" v-if="canStageAll()">
                <div class="dropdown is-hoverable">
                    <div class="dropdown-trigger">
                        <button class="button" aria-haspopup="true" aria-controls="dropdown-menu">
                            <span>Options</span>
                            <span class="icon is-small">
                                <i class="fas fa-angle-down" aria-hidden="true"></i>
                            </span>
                        </button>
                    </div>
                    <div class="dropdown-menu" id="dropdown-menu" role="menu">
                        <div class="dropdown-content">
                            <a @click="doStageAll(true)" class="dropdown-item">
                                Stage all ({{repos.total}})
                            </a>
                        </div>
                        <div class="dropdown-content">
                            <a @click="doStageAll(false)" class="dropdown-item">
                                Un-stage all
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <table class="table is-striped is-narrow is-hoverable is-fullwidth" >
            <thead>
            <tr class="field">
                <th style="width: 6%">Staged</th>
                <th style="width: 6%">Required</th>
                <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchRepos(true)"></th>
                <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchRepos(true)"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="repo in filter()">
                <td align="center"><input type="checkbox" v-model="repo.staged" @change="doStage(repo)" :disabled="!canStage(repo)" /></td>
                <td align="center"><input type="checkbox" v-model="repo.required" disabled /></td>
                <td>{{repo.name}}</td>
                <td>
                    <a :href="repo.descriptor.src" v-if="repo.descriptor.src">{{repo.descriptor.src}}</a>
                    <span v-else><strong>undefined</strong></span>
                </td>
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
                entry: '',
                staged: false
            }
        }
    },
    computed: {
        paginationSettings: {
            get() {
                const vm = this
                return {
                    refresh: function(from) {
                        vm.filters.from = from
                        vm.searchRepos()
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
            const vm = this

            const filtered = []

            vm.repos.descriptors.forEach(function(repo) {
                filtered.push(repo)
            })

            return filtered.sort(compareValues('name'))
        },

        searchRepos: function(fromFilter) {
            const vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.repos.search, vm.filters).then(response => {
                vm.repos = response.data
            })
        },

        toggleSearchOptions: function(option) {
            this.filters[option] = !this.filters[option]
            this.searchRepos(true)
        },

        doStage: function(repo) {
            const vm = this

            if (repo.staged) {
                axios.post(repo.links.stage).then(() => {
                    repo.staged = true
                    vm.repos.staged++
                }).catch(() => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>stage REPO</strong>!`)
                })
            } else {
                axios.post(repo.links.unstage).then(() => {
                    repo.staged = false
                    vm.repos.staged--
                }).catch(() => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>unstage REPO</strong>!`)
                })
            }
        },

        doStageAll: function(stage) {
            const vm = this

            const toggleStaged = function() {
                vm.repos.descriptors.forEach(function(repo) {
                    if (repo.required)
                        return

                    repo.staged = stage
                })
            }

            if (stage)
                axios.post(vm.value.api.links.repos.stageAll).then(_ => {
                    toggleStaged()
                    vm.searchRepos()
                }).catch(() => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>stage all REPOs</strong>!`)
                })
            else
                axios.post(vm.value.api.links.repos.unstageAll).then(_ => {
                    toggleStaged()
                    vm.searchRepos()
                }).catch(() => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>un-stage all REPOs</strong>!`)
                })

        },

        canStage: function(repo) {
            if (!repo) return false
            if (repo.required) return false

            return repo.links.stage
        },

        canStageAll: function() {
            return this.value.api.links.repos.stageAll
        }

    },
    created: function() {
        this.searchRepos()
    }
})