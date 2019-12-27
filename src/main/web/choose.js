Vue.component('choose', {
    template: `
<div>
    <p class="title is-1">Choose: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='select' }"><a @click="activeTab='select'">Select ({{value.invs.count}}/{{value.invs.total}})</a></li>
            <li v-bind:class="{ 'is-active' : activeTab=='summary' }"><a @click="activeTab='summary'">Summary ({{value.invs.selected + value.invs.requiredByAssociation}}/{{value.invs.total}})</a></li>
        </ul>
    </div>

    <choose-select v-model="value" v-show="activeTab=='select'"></choose-select>
    <choose-summary v-model="value" v-show="activeTab=='summary'"></choose-summary>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'select'
        }
    },
    methods: {
    }
})

Vue.component('choose-select', {
    template: `
<div>

    <div class="field is-grouped is-grouped-right">
        <div class="field">
            <button @click="toggleSearchOptions('selected')" v-bind:class="{ 'is-link': filters.selected}" class="button">
                Show only selected
            </button>
            <button @click="toggleSearchOptions('required')" v-bind:class="{ 'is-link': filters.required}" class="button">
                Show all required
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
                            Select all
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

    <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-if="value.invs.nodes">
        <thead>
        <tr class="field">
            <th>Selected</th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"@keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID" @keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source" @keyup="searchNodes(true)"></th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td align="center"><input type="checkbox" v-model="inv.selected" @change="doSelect(inv)" :disabled="inv.required" /></td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>
                <span v-if="inv.scm"><a @click.stop="showScm(inv)">{{inv.scm}}</a></span>
                <span v-else>Not defined</span>
            </td>
        </tr>
        </tbody>
    </table>

    <pagination v-model="paginationSettings" />

    <div class="modal is-active" v-if="viewScm">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 50%">
            <div class="box" v-click-outside="close">
                <h1 class="subtitle is-1">My scm</h1>
                <table class="table is-fullwidth is-bordered">
                    <thead>
                    <tr class="field">
                        <th>Name</th>
                        <th>Source</th>
                        <th>Entry</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>{{viewScm.name}}</td>
                        <td>{{viewScm.descriptor.src}}</td>
                        <td>{{viewScm.descriptor.entry}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
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
                    total: vm.value.invs.count
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.value.invs.nodes.forEach(function(node) {
                filtered.push(node)
            })

            return filtered.sort(compareValues('owner'))
        },

        searchNodes: function(fromFilter) {
            var vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.run.search, vm.filters).then(response => {
                vm.value.invs = response.data
                vm.value.requiredInvs = {}
            })
        },
        toggleSearchOptions: function(option) {
            this.filters[option] = !this.filters[option]
            this.searchNodes(true)
        },
        setStageAll: function(stage) {
            var vm = this

            vm.value.invs.nodes.forEach(function(inv) {
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

            axios.get(inv.links.view).then(response => {
                vm.viewScm = response.data
            })
        },
        close: function() {
            this.viewScm = null
        }
    }
})


Vue.component('choose-summary', {
    template: `
<div>
    <div v-if="value.invs.selected == 0">
        Nothing selected yet...
    </div>
    <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-else>
        <thead>
        <tr class="field">
            <th>Brought by</th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"@keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID" @keyup="searchNodes(true)"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source" @keyup="searchNodes(true)"></th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td v-if="inv.selected">Myself</td>
            <td v-if="inv.required"><a @click.stop="showRequiredBy(inv)">Someone else</a></td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>{{inv.scm}}</td>
            <td></td>
        </tr>
        </tbody>
    </table>

    <pagination v-model="paginationSettings" />

    <div class="modal is-active" v-if="requiredBy && requiredByInvs.nodes">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 80%">
            <div class="box" v-click-outside="close">
                <h1 class="title is-5"> {{requiredByInvs.nodes.length}} require(s)
                    {{requiredBy.name}} (id={{requiredBy.id}})
                </h1>

                <div class="field is-grouped is-grouped-right">
                    <div class="field">
                        <button @click="requiredByFilters.selected = !requiredByFilters.selected" v-bind:class="{ 'is-link': requiredByFilters.selected}" class="button">
                            Show selected only
                        </button>
                    </div>
                </div>

                <table class="table is-narrow is-fullwidth">
                    <thead>
                    <tr class="field">
                        <th>Level</th>
                        <th><input class="input" type="text" v-model="requiredByFilters.owner" placeholder="Owner"></th>
                        <th><input class="input" type="text" v-model="requiredByFilters.name" placeholder="Name"></th>
                        <th><input class="input" type="text" v-model="requiredByFilters.id" placeholder="ID"></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="other in filterRequiredBy()" v-bind:class="{ 'whobroughtme_chosen' : other.selected }">
                        <td>{{(other.iteration / 2) + 1}}</td>
                        <td>{{other.owner}}</td>
                        <td>{{other.name}}</td>
                        <td>{{other.id}}</td>
                    </tr>
                    <tr v-if="requiredByFilteredCount == 0">
                        <td>No matches</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            requiredBy: null,
            requiredByLoading: false,
            requiredByInvs: [],
            filters: {
                required: true,
                from: 0,
                step: 20,
                name: '',
                id: '',
                owner: '',
                scm: ''
            },
            requiredByFilteredCount: 0,
            requiredByFilters: {
                selected: false,
                name: '',
                id: '',
                owner: '',
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
                    total: vm.value.requiredInvs.count
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            if (vm.value.invs.selected == 0)
                return []

            if (vm.value.requiredInvs == undefined || vm.value.invs.selected != vm.value.requiredInvs.selected) {
                vm.searchNodes()
                return
            }

            vm.value.requiredInvs.nodes.forEach(function(node) {
                filtered.push(node)
            })

            return filtered.sort(compareValues('owner'))
        },
        searchNodes: function(fromFilter) {
            var vm = this

            if (vm.value.invs.selected == 0)
                return

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.run.selected, vm.filters).then(response => {
                vm.value.requiredInvs = response.data
            })
        },
        showRequiredBy: function(inv) {
            var vm = this

            vm.requiredBy = inv

            vm.requiredByLoading = true

            axios.get(inv.links.requiredBy).then(response => {
                vm.requiredByInvs = response.data
                vm.requiredByLoading = false
            })
        },
        filterRequiredBy: function() {
            var vm = this

            var filtered = []

            vm.requiredByInvs.nodes.forEach(function(node) {

                if (vm.requiredByFilters.selected && !node.selected) return false
                if (vm.requiredByFilters.owner != '' && node.owner.indexOf(vm.requiredByFilters.owner) < 0) return false
                if (vm.requiredByFilters.name != '' && node.name.indexOf(vm.requiredByFilters.name) < 0) return false
                if (vm.requiredByFilters.id != '' && node.id.indexOf(vm.requiredByFilters.id) < 0) return false

                filtered.push(node)
            })

            vm.requiredByFilteredCount = filtered.length

            return filtered.sort((a, b) => (a.iteration - b.iteration))
        },
        close: function() {
            this.requiredBy = null
            this.requiredByInvs = null
        }

    }
})

