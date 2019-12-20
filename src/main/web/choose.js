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
    <table class="table is-fullwidth" v-if="value.invs.nodes">
        <thead>
        <tr class="field">
            <th class="checkbox_header"><input type="checkbox" v-model="selectAll" @click="doSelectAll()" /></th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"@keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID" @keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source" @keyup="searchNodes()"></th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td align="center"><input type="checkbox" v-model="inv.selected" @change="doSelect(inv)" :disabled="inv.required" /></td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>
                <span v-if="value.scms.registry[inv.scm]"><a @click.stop="viewScm = value.scms.registry[inv.scm]">{{inv.scm}}</a></span>
                <span v-else>{{inv.scm}}</span>
            </td>
            <td></td>
        </tr>
        </tbody>
    </table>

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
            selectAll: false,
            viewScm: null,
            filters: {
                name: '',
                id: '',
                owner: '',
                scm: ''
            }
        }
    },
    created: function(){
        this.searchNodes()
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
        searchNodes: function() {
            var vm = this

            var link = "/run"

            if (vm.value.invs.links != undefined)
                link = vm.value.invs.links.search

            axios.post(link, vm.filters).then(response => {
                vm.value.invs = response.data
                //vm.$forceUpdate()
            })
        },
        doSelectAll: function() {
            var vm = this

            vm.selectAll = !vm.selectAll

            vm.value.invs.nodes.forEach(function(inv) {
                if (inv.required)
                    return

                inv.selected = vm.selectAll

                vm.doSelect(inv)
            })

        },
        doSelect: function(inv) {
            var vm = this

            if (inv.selected) {
                axios.post(inv.links.stage, vm.filters).then(response => {
                    vm.value.invs = response.data
                    vm.value.requiredInvs = response.data
                })
            } else {
                axios.post(inv.links.unstage, vm.filters).then(response => {
                    vm.value.invs = response.data
                    vm.value.requiredInvs = response.data
                })
            }
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
    <table class="table is-fullwidth" v-else>
        <thead>
        <tr class="field">
            <th>Brought by</th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner" @keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"@keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID" @keyup="searchNodes()"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source" @keyup="searchNodes()"></th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td v-if="inv.selected">Myself</td>
            <td v-if="inv.required"><a @click.stop="showRequiredBy(inv)">Someone else</td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>{{inv.scm}}</td>
            <td></td>
        </tr>
        </tbody>
    </table>

    <div class="modal is-active" v-if="requiredBy">
        <div class="modal-background"></div>
        <div class="modal-content" style="width: 80%">
            <div class="box" v-click-outside="close">
                <h1 class="subtitle is-1">
                    Required by:
                    <span class="icon is-small" v-if="requiredByLoading">
                        <i class="fas fa-spinner fa-pulse"></i>
                    </span>
                </h1>
                <table class="table is-fullwidth is-bordered">
                    <thead>
                    <tr class="field">
                        <th>Owner</th>
                        <th>Name</th>
                        <th>ID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="other in requiredByInvs.nodes" v-bind:class="{ 'whobroughtme_chosen' : other.selected }">
                        <td>{{other.owner}}</td>
                        <td>{{other.name}}</td>
                        <td>{{other.id}}</td>
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
                name: '',
                id: '',
                owner: '',
                scm: ''
            }
        }
    },
    created: function() {
        this.searchNodes()
    },
    methods: {
        filter: function() {
            var vm = this

            var vm = this

            var filtered = []

            if (vm.value.requiredInvs.nodes == undefined)
                return []

            vm.value.requiredInvs.nodes.forEach(function(node) {
                filtered.push(node)
            })

            return filtered.sort(compareValues('owner'))
        },
        searchNodes: function() {
            var vm = this

            var link = "/run/selected"

            if (vm.value.invs.links != undefined)
                link = vm.value.invs.links.selected

            axios.post(link, vm.filters).then(response => {
                vm.value.requiredInvs = response.data
                //vm.$forceUpdate()
            })
        },
        showRequiredBy: function(inv) {
            var vm = this

            vm.requiredBy = inv

            vm.requiredByLoading = true

            axios.get(inv.links.requiredBy).then(response => {
                vm.requiredByInvs = response.data
                //vm.$forceUpdate()

                vm.requiredByInvs.nodes.sort(compareValues('owner'))

                vm.requiredByLoading = false
            })
        },
        close: function() {
            this.requiredBy = null
        }

    }
})

