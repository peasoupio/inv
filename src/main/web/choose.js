Vue.component('choose', {
    template: `
<div>
    <p class="title is-1">Choose: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='select' }"><a v-on:click="activeTab='select'">Select ({{value.invs.nodes.length}})</a></li>
            <li v-bind:class="{ 'is-active' : activeTab=='summary' }"><a v-on:click="activeTab='summary'">Summary ({{chosenSize()}})</a></li>
        </ul>
    </div>

    <choose-select v-model="value" v-if="activeTab=='select'"></choose-select>
    <choose-summary v-model="value" v-if="activeTab=='summary'"></choose-summary>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'select'
        }
    },
    methods: {
        chosenSize: function() {
            var vm = this

            var count = 0

            vm.value.invs.nodes.forEach(function(inv) {
                if (inv.chosen) {
                    count++
                    return
                }

                if (inv.broughtBySomeone.length > 0) {
                    count++
                    return
                }
            })

            return count
        }
    }
})

Vue.component('choose-select', {
    template: `
<div>
    <table class="table is-fullwidth">
        <thead>
        <tr class="field">
            <th class="checkbox_header"><input type="checkbox" v-model="selectAll" v-on:click="doSelectAll()" /></th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source"></th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td align="center"><input type="checkbox" v-model="inv.chosen" v-on:change="doSelect(inv)" /></td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>
                <span v-if="value.scms.registry[inv.scm]"><a v-on:click="viewScm = value.scms.registry[inv.scm]">{{inv.scm}}</a></span>
                <span v-else>{{inv.scm}}</span>
            </td>
            <td></td>
        </tr>
        </tbody>
    </table>

    <div class="modal is-active" v-if="viewScm">
        <div class="modal-background"></div>
        <div class="modal-content">
            <div class="box">
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
        <button class="modal-close is-large" aria-label="close" v-on:click="viewScm = null"></button>
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
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.value.invs.nodes.forEach(function(inv) {
                if (vm.filters.name != '' && inv.name.indexOf(vm.filters.name) < 0) return
                if (vm.filters.id != '' && inv.id.indexOf(vm.filters.id) < 0) return
                if (vm.filters.owner != '' && inv.owner.indexOf(vm.filters.owner) < 0) return
                if (vm.filters.scm != '' && inv.scm.indexOf(vm.filters.scm) < 0) return

                filtered.push(inv)
            })

            return filtered.sort(compareValues('owner'))

        },
        doSelectAll: function() {
            var vm = this

            vm.selectAll = !vm.selectAll

            vm.value.invs.nodes.forEach(function(inv) {
                inv.chosen = vm.selectAll

                vm.doSelect(inv)
            })

        },
        doSelect: function(inv) {
            var vm = this

            if (inv.chosen) {
                axios.post(vm.value.invs.links.stage[inv.owner]).then(response => {
                    vm.value.invs = response.data
                    //vm.$forceUpdate()
                })
            } else {
                axios.post(vm.value.invs.links.unstage[inv.owner]).then(response => {
                    vm.value.invs = response.data
                    //vm.$forceUpdate()
                })
            }
        }
    }
})


Vue.component('choose-summary', {
    template: `
<div>
    <div v-if="count == 0">
        Nothing selected yet...
    </div>
    <table class="table is-fullwidth">
        <thead v-if="count > 0">
        <tr class="field">
            <th>Brought by</th>
            <th><input class="input" type="text" v-model="filters.owner" placeholder="Owner"></th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"></th>
            <th><input class="input" type="text" v-model="filters.id" placeholder="ID"></th>
            <th><input class="input" type="text" v-model="filters.scm" placeholder="Source"></th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="inv in filter()">
            <td v-if="inv.chosen">Myself</td>
            <td v-if="!inv.chosen"><a v-on:click="showWhoBroughtBy = inv.owner">Someone else</td>
            <td>{{inv.owner}}</td>
            <td>{{inv.name}}</td>
            <td>{{inv.id}}</td>
            <td>{{inv.scm}}</td>
            <td></td>
        </tr>
        </tbody>
    </table>

    <div class="modal" v-bind:class="{ 'is-active': showWhoBroughtBy != '' }">
        <div class="modal-background"></div>
        <div class="modal-content">
            <div class="box">
                <h1 class="subtitle is-1">Who brought me?</h1>
                <table class="table is-fullwidth is-bordered">
                    <thead>
                    <tr class="field">
                        <th>Owner</th>
                        <th>Name</th>
                        <th>ID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr v-for="other in whoBroughtMe(showWhoBroughtBy)" v-bind:class="{ 'whobroughtme_chosen' : other.chosen }">
                        <td>{{other.owner}}</td>
                        <td>{{other.name}}</td>
                        <td>{{other.id}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <button class="modal-close is-large" aria-label="close" v-on:click="showWhoBroughtBy = ''"></button>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            count: 0,
            showWhoBroughtBy: '',
            selectAll: false,
            filters: {
                name: '',
                id: '',
                owner: '',
                scm: ''
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.value.invs.nodes.forEach(function(inv) {

                if (!inv.chosen && inv.broughtBySomeone.length == 0) return

                if (vm.filters.name != '' && inv.name.indexOf(vm.filters.name) < 0) return
                if (vm.filters.id != '' && inv.id.indexOf(vm.filters.id) < 0) return
                if (vm.filters.owner != '' && inv.owner.indexOf(vm.filters.owner) < 0) return
                if (vm.filters.scm != '' && inv.scm.indexOf(vm.filters.scm) < 0) return

                filtered.push(inv)
            })

            vm.count = filtered.length

            return filtered.sort(compareValues('owner'))
        },


        whoBroughtMe: function(owner) {
            var vm = this

            if (owner == '')
                return

            var cache = {}

            vm.value.invs.nodes.forEach(function(inv) {
                cache[inv.owner] = inv
            })

            var myself = cache[owner]
            var whoBroughtMe = []

            myself.broughtBySomeone.forEach(function(otherName) {

                var other = cache[otherName]

                if (whoBroughtMe.indexOf(other) > -1)
                    return

                whoBroughtMe.push(other)
            })

            return whoBroughtMe.sort(compareValues('owner'))
        },

    }
})

