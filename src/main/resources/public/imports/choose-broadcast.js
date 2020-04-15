Vue.component('choose-broadcast', {
    template: `
<div>
    <choose-not-available v-if="notAvailable"></choose-not-available>
    <div v-else>
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="toggleSearchOptions('selected')" v-bind:class="{ 'is-link': filters.selected}" class="button breath">
                    Show only selected ({{invs.selected}}/{{invs.count}})
                </button>
                <button @click="toggleSearchOptions('required')" v-bind:class="{ 'is-link': filters.required}" class="button breath">
                    Show all required ({{invs.requiredByAssociation}}/{{invs.count}})
                </button>
            </div>
            <div class="field">
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
                            <a @click="setStageAll(true)" class="dropdown-item">
                                Select all ({{invs.total}})
                            </a>
                        </div>
                        <div class="dropdown-content">
                            <a @click="setStageAll(false)" class="dropdown-item">
                                Un-select all
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-if="invs.nodes">
            <thead>
            <tr class="field">
                <th style="width: 6%">Selected</th>
                <th style="width: 20%">
                <div class="dropdown" v-bind:class="{ 'is-active': filterOwners().length > 0 }" style="width: 100%">
                    <div class="dropdown-trigger" style="width: 100%">
                        <div class="field">
                            <p class="control is-expanded has-icons-right">
                                <input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes(true)">
                                <span class="icon is-small is-right"><i class="fas fa-search"></i></span>
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
                                <input class="input" type="text" v-model="filters.name" placeholder="Name"@keyup="searchNodes(true)">
                                <span class="icon is-small is-right"><i class="fas fa-search"></i></span>
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
                <th><input class="input" type="text" v-model="filters.scm" placeholder="Source" @keyup="searchNodes(true)"></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="inv in filter()">
                <td align="center"><input type="checkbox" v-model="inv.selected" @change="doSelect(inv)" :disabled="inv.required" /></td>
                <td><p class="truncate">{{inv.owner}}</p></td>
                <td><p class="truncate">{{inv.name}}</p></td>
                <td><p class="truncate">{{inv.id}}</p></td>
                <td>
                    <span class="truncate" v-if="inv.scm"><a @click.stop="showScm(inv)">{{inv.scm}}</a></span>
                    <span v-else>Not defined</span>
                </td>
            </tr>
            </tbody>
        </table>
        <pagination v-model="paginationSettings" />

        <div class="modal is-active" v-if="viewScm">
            <div class="modal-background"></div>
            <div class="modal-content" style="width: 60%">
                <div class="box" v-click-outside="close">
                    <h1 class="subtitle is-1">My scm</h1>
                    <table class="table is-fullwidth is-bordered">
                        <thead>
                        <tr class="field">
                            <th style="width: 30%">Name</th>
                            <th>Source</th>
                            <th style="width: 30%">Entry</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr>
                            <td><p class="truncate">{{viewScm.name}}</p></td>
                            <td><p class="truncate">{{viewScm.descriptor.src}}</p></td>
                            <td><p class="truncate" v-for="entry in viewScm.descriptor.entry">{{entry}}</p></td>
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
            viewScm: null,
            filters: {
                from: 0,
                step: 20,
                name: '',
                id: '',
                owner: '',
                scm: '',
                required: false,
                selected: false
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
        selectOwnerFilterRecommendation: function(owner) {
            var vm = this

            vm.filters.owner = owner
            vm.searchNodes(true)
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
        selectNameFilterRecommendation: function(name) {
            var vm = this

            vm.filters.name = name
            vm.searchNodes(true)
        },

        setStageAll: function(stage) {
            var vm = this

            vm.invs.nodes.forEach(function(inv) {
                if (inv.required)
                    return

                inv.selected = stage
            })

            if (stage)
                axios.post(vm.value.api.links.run.stageAll, vm.filters).then(response => {
                    vm.searchNodes()
                })
            else
                axios.post(vm.value.api.links.run.unstageAll, vm.filters).then(response => {
                    vm.searchNodes()
                })

        },
        doSelect: function(inv) {
            var vm = this

            if (inv.selected) {
                axios.post(inv.links.stage, vm.filters).then(response => {
                    vm.searchNodes()
                })
            } else {
                axios.post(inv.links.unstage, vm.filters).then(response => {
                    vm.searchNodes()
                })
            }
        },

        showScm: function(inv) {
            var vm = this

            axios.get(inv.links.viewScm).then(response => {
                vm.viewScm = response.data
            })
        },
        close: function() {
            this.viewScm = null
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