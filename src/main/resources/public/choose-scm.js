Vue.component('choose-scm', {
    template: `
<div>

    <div v-if="count == 0">
        No SCMS available yet...
    </div>
    <div v-else>

        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="stageAll()"class="button" style="margin-right: 1em;">
                    Stage all
                </button>
            </div>
            <div class="field">
                <button @click="unstageAll()"class="button">
                    Unstage all
                </button>
            </div>
        </div>

        <hr />

        <table class="table is-striped is-narrow is-hoverable is-fullwidth" >
            <thead>
            <tr class="field">
                <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchScm(true)"></th>
                <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchScm(true)"></th>
                <th><input class="input" type="text" v-model="filters.entry" placeholder="Entry" @keyup="searchScm(true)"></th>
                <th style="width: 10%">Options</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="scm in filter()">
                <td>{{scm.name}}</td>
                <td>{{scm.descriptor.src}}</td>
                <td><p v-for="entry in scm.descriptor.entry">{{entry}}</p></td>
                <td v-if="!scm.staged"><button class="button is-link" @click.stop="stage(scm)" :disabled="scm.selected">Stage</button></td>
                <td v-if="scm.staged"><button class="button is-success" @click.stop="unstage(scm)">Unstage</button></td>
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
            count: this.value.scms.length,
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
                        vm.searchScm()
                    },
                    from: vm.filters.from,
                    step: vm.filters.step,
                    total: vm.value.scms.total
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.value.scms.descriptors.forEach(function(scm) {
                filtered.push(scm)
            })

            return filtered.sort(compareValues('name'))
        },
        searchScm: function(fromFilter) {
            var vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.scms.search, vm.filters).then(response => {
                vm.value.scms = response.data
            })
        },
        stage: function(scm) {
            axios.post(scm.links.stage).then(response => {
                scm.staged = true
            })
        },
        unstage: function(scm) {
            axios.post(scm.links.unstage).then(response => {
                scm.staged = false
            })
        },
        stageAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.stageAll).then(response => {
                vm.searchScm()
            })
        },
        unstageAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.unstageAll).then(response => {
                vm.searchScm()
            })
        }
    }
})