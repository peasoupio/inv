Vue.component('choose-broadcast', {
    template: `
<div>
    <choose-not-available v-if="notAvailable"></choose-not-available>
    <div v-else>
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="toggleSearchOptions('staged')" v-bind:class="{ 'is-link': filters.staged}" class="button breath">
                    Show only staged ({{invs.staged}}/{{invs.count}})
                </button>
                <button @click="toggleSearchOptions('required')" v-bind:class="{ 'is-link': filters.required}" class="button breath">
                    Show all required ({{invs.required}}/{{invs.count}})
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
                                Stage all ({{invs.total}})
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
        <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-if="invs.nodes">
            <thead>
            <tr class="field">
                <th style="width: 6%">Staged</th>
                <th style="width: 6%">Required</th>
                <th style="width: 20%">
                <div class="dropdown" v-bind:class="{ 'is-active': filterOwners().length > 0 }" style="width: 100%">
                    <div class="dropdown-trigger" style="width: 100%">
                        <div class="field">
                            <p class="control is-expanded has-icons-right">
                                <input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes(true)">
                                <span class="icon is-small is-right"><i class="fas fa-list"></i></span>
                            </p>
                        </div>
                    </div>
                    <div class="dropdown-menu" id="dropdown-menu" role="menu">
                        <div class="dropdown-content">
                            <a v-for="owner in filterOwners().slice(0,5)" @click="selectOwnerFilterRecommendation(owner)" class="dropdown-item">{{owner}}</a>
                        </div>
                    </div>
                </div>
                </th>
                <th style="width: 14%">
                <div class="dropdown" v-bind:class="{ 'is-active': filterNames().length > 0 }" style="width: 100%">
                    <div class="dropdown-trigger" style="width: 100%">
                        <div class="field">
                            <p class="control is-expanded has-icons-right">
                                <input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchNodes(true)">
                                <span class="icon is-small is-right"><i class="fas fa-list"></i></span>
                            </p>
                        </div>
                    </div>
                    <div class="dropdown-menu" id="dropdown-menu" role="menu">
                        <div class="dropdown-content">
                            <a v-for="owner in filterNames().slice(0,5)" @click="selectNameFilterRecommendation(owner)" class="dropdown-item">{{owner}}</a>
                        </div>
                    </div>
                </div>
                </th>
                <th><input class="input" type="text" v-model="filters.id" placeholder="ID" @keyup="searchNodes(true)"></th>
                <th><input class="input" type="text" v-model="filters.repo" placeholder="Source" @keyup="searchNodes(true)"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="inv in filter()">
                <td align="center"><input type="checkbox" v-model="inv.staged" @change="doStage(inv)" :disabled="!canStage(inv)" /></td>
                <td align="center"><input type="checkbox" v-model="inv.required" disabled /></td>
                <td><p class="truncate">{{inv.owner}}</p></td>
                <td><p class="truncate">{{inv.name}}</p></td>
                <td><p class="truncate">{{inv.id}}</p></td>
                <td>
                    <span class="truncate" v-if="inv.repo"><a @click.stop="showRepo(inv)">{{inv.repo}}</a></span>
                    <span v-else>Not defined</span>
                </td>
            </tr>
            </tbody>
        </table>

        <pagination v-model="paginationSettings" />

        <div class="modal is-active" v-if="viewRepo">
            <div class="modal-background"></div>
            <div class="modal-content" style="width: 60%">
                <div class="box" v-click-outside="close">
                    <h1 class="subtitle is-1">My Repo</h1>
                    <table class="table is-fullwidth is-bordered">
                        <thead>
                        <tr class="field">
                            <th style="width: 30%">Name</th>
                            <th>Source</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td><p class="truncate">{{viewRepo.name}}</p></td>
                            <td><p class="truncate"><a :href="viewRepo.descriptor.src" target="_blank">{{viewRepo.descriptor.src}}</a></p></td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            invs: {},
            notAvailable: false,
            owners: [],
            names: [],
            viewRepo: null,
            filters: {
                from: 0,
                step: 20,
                name: '',
                id: '',
                owner: '',
                repo: '',
                required: false,
                staged: false
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
                        vm.searchNodes()
                    },
                    from: vm.filters.from,
                    step: vm.filters.step,
                    total: vm.invs.count
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.invs.nodes.forEach(function(node) {
                filtered.push(node)
            })

            return filtered.sort(compareValues('owner'))
        },

        searchNodes: function(fromFilter) {
            var vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.run.search, vm.filters).then(response => {
                vm.invs = response.data
                vm.value.requiredInvs = {}
            })
        },

        toggleSearchOptions: function(option) {
            this.filters[option] = !this.filters[option]
            this.searchNodes(true)
        },

        filterOwners: function() {
            var vm = this

            if (vm.invs.owners.length == 0)
                return []

            if (vm.filters.owner == '')
                return []

            var filtered = []

           vm.invs.owners.forEach(function(owner) {
                if (vm.filters.owner && owner.indexOf(vm.filters.owner) < 0) return

                filtered.push(owner)
            })

            if (filtered.length == 1)
                return []

            return filtered.sort()
        },

        filterNames: function() {
            var vm = this

            if (vm.invs.names.length == 0)
                return []

            if (vm.filters.name == '')
                return []

            var filtered = []

            vm.invs.names.forEach(function(name) {
                if (vm.filters.name && name.indexOf(vm.filters.name) < 0) return

                filtered.push(name)
            })

            if (filtered.length == 1)
                return []

            return filtered.sort()
        },

        selectOwnerFilterRecommendation: function(owner) {
            var vm = this

            vm.filters.owner = owner
            vm.searchNodes(true)
        },


        selectNameFilterRecommendation: function(name) {
            var vm = this

            vm.filters.name = name
            vm.searchNodes(true)
        },

        doStage: function(inv) {
            var vm = this

            if (inv.staged) {
                axios.post(inv.links.stage, vm.filters).then(response => {
                    vm.searchNodes()
                }).catch(err => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>stage broadcast</strong>!`)
                })
            } else {
                axios.post(inv.links.unstage, vm.filters).then(response => {
                    vm.searchNodes()
                }).catch(err => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>unstage broadcast</strong>!`)
                })
            }
        },

        doStageAll: function(stage) {
            var vm = this

            var toggleStaged = function() {
                vm.invs.nodes.forEach(function(inv) {
                    if (inv.required)
                        return

                    inv.staged = stage
                })
            }

            if (stage)
                axios.post(vm.value.api.links.run.stageAll, vm.filters).then(response => {
                    vm.searchNodes()
                    toggleStaged()
                }).catch(err => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>stage all broadcasts</strong>!`)
                })

            else
                axios.post(vm.value.api.links.run.unstageAll, vm.filters).then(response => {
                    vm.searchNodes()
                    toggleStaged()
                }).catch(err => {
                    vm.$bus.$emit('toast', `error:Failed to <strong>unstage all broadcasts</strong>!`)
                })

        },

        showRepo: function(inv) {
            var vm = this

            axios.get(inv.links.viewRepo).then(response => {
                vm.viewRepo = response.data
            })
            .catch(err => {
                vm.$bus.$emit('toast', `error:${err.response.data.message}!`)
            })
        },

        close: function() {
            this.viewRepo = null
        },

        canStageAll: function() {
            return this.value.api.links.run.stageAll
        },

        canStage: function(inv) {
            if (!inv) return false
            if (inv.required) return false

            return inv.links.stage
        }
    },
    created: function() {
        var vm = this

        axios.get(vm.value.api.links.run.default).then(response => {
            vm.invs = response.data
        })
        .catch(err => {
            vm.notAvailable = true
        })
    }

})