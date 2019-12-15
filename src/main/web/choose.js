Vue.component('choose', {
    template: `
<div>
    <p class="title is-1">Choose: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='find' }"><a v-on:click="activeTab='find'">Find ({{value.availables.length}})</a></li>
            <li v-bind:class="{ 'is-active' : activeTab=='summary' }"><a v-on:click="activeTab='summary'">Summary ({{chosenSize()}})</a></li>
        </ul>
    </div>

    <choose-find v-model="value" v-if="activeTab=='find'"></choose-find>
    <choose-summary v-model="value" v-if="activeTab=='summary'"></choose-summary>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'find'
        }
    },
    methods: {
        chosenSize: function() {
            var vm = this

            var count = 0

            vm.value.availables.forEach(function(available) {
                if (available.chosen) {
                    count++
                    return
                }

                if (available.broughtBySomeone) {
                    count++
                    return
                }
            })

            return count
        }
    }
})

Vue.component('choose-find', {
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
        <tr v-for="available in filter()">
            <td align="center"><input type="checkbox" v-model="available.chosen" v-on:change="traverseRequired()" /></td>
            <td>{{available.owner}}</td>
            <td>{{available.name}}</td>
            <td>{{available.id}}</td>
            <td>
                <span v-if="value.scms.registry[available.scm]"><a v-on:click="viewScm = value.scms.registry[available.scm]">{{available.scm}}</a></span>
                <span v-else>{{available.scm}}</span>
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

            vm.value.availables.forEach(function(available) {
                if (vm.filters.name != '' && available.name.indexOf(vm.filters.name) < 0) return
                if (vm.filters.id != '' && available.id.indexOf(vm.filters.id) < 0) return
                if (vm.filters.owner != '' && available.owner.indexOf(vm.filters.owner) < 0) return
                if (vm.filters.scm != '' && available.scm.indexOf(vm.filters.scm) < 0) return

                filtered.push(available)
            })

            return filtered.sort(compareValues('owner'))

        },
        doSelectAll: function() {
            var vm = this

            vm.selectAll = !vm.selectAll

            vm.value.availables.forEach(function(available) {
                available.chosen = vm.selectAll
            })

            // Overkill, but works for the moment
            vm.traverseRequired()
        },
        traverseRequired: function(){
            var vm = this

            // reset
            vm.value.availables.forEach(function(value) {
                value.broughtBySomeone = false
            })

            vm.value.availables.forEach(function(value) {
                if (!value.chosen)
                    return

                vm.bringPeopleWithMe(value).forEach(function(other) {
                    other.broughtBySomeone = true
                })
            })
        },
        bringPeopleWithMe: function(myself) {
            var vm = this

            var others = []

            myself.required.forEach(function(require) {
                vm.value.availables.forEach(function(available) {
                    if (require.owner == available.owner) {
                        others.push(available)
                    }
                })
            })

            return others
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
        <tr v-for="available in filter()">
            <td v-if="available.chosen">Myself</td>
            <td v-if="!available.chosen"><a v-on:click="showWhoBroughtBy = available.owner">Someone else</td>
            <td>{{available.owner}}</td>
            <td>{{available.name}}</td>
            <td>{{available.id}}</td>
            <td>{{available.scm}}</td>
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

            vm.value.availables.forEach(function(available) {

                if (!available.chosen && !available.broughtBySomeone) return

                if (vm.filters.name != '' && available.name.indexOf(vm.filters.name) < 0) return
                if (vm.filters.id != '' && available.id.indexOf(vm.filters.id) < 0) return
                if (vm.filters.owner != '' && available.owner.indexOf(vm.filters.owner) < 0) return
                if (vm.filters.scm != '' && available.scm.indexOf(vm.filters.scm) < 0) return

                filtered.push(available)
            })

            vm.count = filtered.length

            return filtered.sort(compareValues('owner'))
        },


        whoBroughtMe: function(myself) {
            var vm = this

            var whoBroughtMe = []

            vm.value.availables.forEach(function(chosen) {

                if (!chosen.chosen && !chosen.broughtBySomeone)
                    return

                chosen.required.forEach(function(required) {
                    if (required.owner != myself)
                        return

                    if (whoBroughtMe.indexOf(chosen) > -1)
                        return

                    whoBroughtMe.push(chosen)
                })
            })

            return whoBroughtMe.sort(compareValues('owner'))
        },

    }
})

