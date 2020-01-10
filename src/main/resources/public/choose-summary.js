Vue.component('choose-summary', {
    template: `
<div>
    <div v-if="!value.requiredInvs.nodes">
        Nothing selected yet...
    </div>
    <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-else>
        <thead>
        <tr class="field">
            <th style="width: 8%">Brought by</th>
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
            <th style="width: 15%">
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
            <td v-if="inv.selected">Myself</td>
            <td v-if="inv.required"><a @click.stop="showRequiredBy(inv)">Someone else</a></td>
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

    <div class="modal is-active" v-if="requiredBy && requiredByInvs.nodes">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 80%">
            <div class="box" v-click-outside="closeRequiredBy">
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
                        <th style="width: 5%">Level</th>
                        <th style="width: 30%"><input class="input" type="text" v-model="requiredByFilters.owner" placeholder="Owner"></th>
                        <th style="width: 15%"><input class="input" type="text" v-model="requiredByFilters.name" placeholder="Name"></th>
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

    <div class="modal is-active" v-if="viewScm">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 50%">
            <div class="box" v-click-outside="closeScm">
                <h1 class="subtitle is-1">My scm</h1>
                <table class="table is-fullwidth is-bordered">
                    <thead>
                    <tr class="field">
                        <th style="width: 30%">Name</th>
                        <th>Source</th>
                        <th style="width: 20%">Entry</th>
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
            },
            viewScm: null
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

            if (vm.value.requiredInvs == undefined) {
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
        filterOwners: function() {
            var vm = this

            if (vm.value.requiredInvs.owners.length == 0)
                return []

            if (vm.filters.owner == '')
                return []

            var filtered = []

            vm.value.requiredInvs.owners.forEach(function(owner) {
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

            if (vm.value.requiredInvs.names.length == 0)
                return []

            if (vm.filters.name == '')
                return []

            var filtered = []

            vm.value.requiredInvs.names.forEach(function(name) {
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
        closeRequiredBy: function() {
            this.requiredBy = null
            this.requiredByInvs = null
        },
        showScm: function(inv) {
            var vm = this

            axios.get(inv.links.viewScm).then(response => {
                vm.viewScm = response.data
            })
        },
        closeScm: function() {
            this.viewScm = null
        }

    },
    created: function() {
        this.searchNodes()
    }
})