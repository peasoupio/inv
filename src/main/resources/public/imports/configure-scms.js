Vue.component('configure-scms', {
    template: `
<div>
    <div class="modal" v-bind:class="{ 'is-active': value.visible }">
        <div class="modal-background"></div>
        <div class="modal-content configure-scms-modal">
            <div class="box" v-click-outside="close">
                <configure-scms-details v-model="value.shared" />
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            opened: false
        }
    },
    methods: {
        close: function() {
            var vm = this

            vm.value.visible = false
        }
    }
})

Vue.component('configure-scms-details', {
    template: `
<div>
    <div v-if="value.scms.total" >

        <div class="field is-grouped is-grouped-right" v-if="false">
            <div class="field">
                <button @click="openAdd()" class="button breath is-link">
                    Add new SCM file
                </button>
            </div>
        </div>

        <table class="table is-striped is-narrow is-hoverable is-fullwidth">
            <thead>
            <tr class="field">
                <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchScm(true)"></th>
                <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchScm(true)"></th>
                <th><input class="input" type="text" v-model="filters.entry" placeholder="Entry" @keyup="searchScm(true)"></th>
                <th style="width: 8%">Timeout</th>
                <th style="width: 10%">Options</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="scm in filter()">
                <td><span>{{scm.name}}</span><br/><span style="color: lightgrey">Last edit: {{whenLastEdit(scm)}}</span></td>
                <td>{{scm.descriptor.src}}</td>
                <td><p v-for="entry in scm.descriptor.entry">{{entry}}</p></td>
                <td>{{scm.descriptor.timeout}}</td>
                <td>
                    <button class="button is-link breathe" @click.stop="openEdit(scm)">
                        <span class="icon is-small">
                          <i class="fas fa-edit"></i>
                        </span>
                    </button>
                    <button class="button is-danger is-outlined breathe" @click.stop="removeSCM(scm)" v-if="false">
                        <span class="icon is-small">
                          <i class="fas fa-trash"></i>
                        </span>
                    </button>
                </td>
            </tr>
            </tbody>
        </table>

        <pagination v-model="paginationSettings" />
    </div>

    <div class="modal" v-bind:class=" { 'is-active': editScript } ">
        <div class="modal-background"></div>
        <div class="modal-content code-edit-modal">
            <div class="box" v-click-outside="closeEdit">
                <div class="columns" v-if="editScript">
                    <div class="column">
                        <h1 class="title is-3">Edit SCM</h1>
                        <h4 class="subtitle is-6">source: {{editScript.descriptor.src}}</h4>
                    </div>
                    <div class="column is-one-fifth">
                        <div class="buttons has-addons is-right">
                            <button class="button is-success" @click="saveSource()" v-bind:class=" { 'is-loading': sending }" :disabled="!edited">
                                <span class="icon is-small" v-if="saved">
                                    <i class="fas fa-check"></i>
                                </span>
                                <span>Save</span>
                            </button>
                        </div>
                    </div>
                </div>

                <div id="configure-scms-editarea"></div>

                <div v-if="errorCount > 0">
                    <p>Compilation error(s) caught:</p>
                    <p class="has-text-danger" v-for="error in errors">{{error}}</p>
                </div>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            // Table
            count: this.value.scms.length,
            //showWhoBroughtMe: null,
            filters: {
                selected: true,
                from: 0,
                to: 5,
                name: '',
                src: '',
                entry: ''
            },

            // Code editing
            codeMirror: null,
            opened: false,
            editScript: '',
            edited: false,
            sending: false,
            saved: false,
            errorCount: 0,
            errors: []
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
                    step: vm.filters.to,
                    total: vm.value.scms.count
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
        whenLastEdit: function(scm) {
            return TimeAgo.inWords(scm.script.lastEdit)
        },
        openEdit: function(scm) {
            var vm = this

            axios.get(scm.links.default).then(response => {
                var latestScm = response.data

                vm.codeMirror.setValue(latestScm.script.text)
                vm.codeMirror.refresh()

                vm.editScript = latestScm

                vm.edited = false
            })
        },
        saveSource: function() {
            var vm = this

            if (vm.sending)
                return

            vm.sending = true

            var content = vm.codeMirror.getValue()

            axios.post(vm.editScript.links.save, content, {
                headers: { 'Content-Type': 'text/plain' }
            }).then(response => {

                vm.errorCount = response.data.errorCount
                vm.errors = response.data.errors

                vm.sending = false
                vm.edited = false

                if (vm.errorCount == 0) {
                    vm.saved = true
                    vm.searchScm()
                }
            })
        },
        closeEdit: function() {

            var vm = this

            if (vm.edited && !vm.saved) {
                var ok = confirm("Clear unsaved work ?")

                if (!ok)
                    return
            }

            vm.editScript = null

            vm.sending = false
            vm.saved = false
            vm.edited = false
        }
    },
    mounted: function() {
        var vm = this

        var codeMirror = CodeMirror(function(elt) {
            var element = document.querySelector('#configure-scms-editarea')
            element.appendChild(elt)
        }, {
            autoRefresh: true,
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-groovy"
        })

        codeMirror.setSize(null, 640)

        codeMirror.on("change",function(cm,change){
            vm.saved = false
            vm.edited = true
        })

        vm.codeMirror = codeMirror
        vm.searchScm()
    }
})