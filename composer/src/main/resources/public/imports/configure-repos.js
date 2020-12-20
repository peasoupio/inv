Vue.component('configure-repos', {
    template: `
<div>
    <div class="modal" v-bind:class="{ 'is-active': value.visible }">
        <div class="modal-background"></div>
        <div class="modal-content configure-repos-modal">
            <div class="box" v-click-outside="close">
                <p class="title is-6">Edit REPO(s):</p>
                <configure-repos-details v-model="value.shared" />
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

            // Reload to get latest data
            window.location.reload(true)
        }
    }
})

Vue.component('configure-repos-details', {
    template: `
<div>
    <div v-if="repos.descriptors">
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click.stop="openAdd()" class="button breath is-link">
                    Add new REPO file
                </button>
            </div>
        </div>

        <table class="table is-striped is-narrow is-hoverable is-fullwidth">
            <thead>
            <tr class="field">
                <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchRepo(true)"></th>
                <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchRepo(true)"></th>
                <th style="width: 8%">Timeout</th>
                <th style="width: 10%">Options</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="repo in filter()">
                <td><span>{{repo.name}}</span><br/><span style="color: lightgrey">Last edit: {{whenLastEdit(repo)}}</span></td>
                <td>
                    <a :href="repo.descriptor.src" v-if="repo.descriptor.src">{{repo.descriptor.src}}</a>
                    <span v-else><strong>undefined</strong></span>
                </td>
                <td>{{repo.descriptor.timeout}}</td>
                <td>
                    <button class="button is-link breathe" @click.stop="openEdit(repo)">
                        <span class="icon is-small">
                          <i class="fas fa-edit"></i>
                        </span>
                    </button>
                    <button class="button is-danger is-outlined breathe" @click.stop="removeREPO(repo)">
                        <span class="icon is-small">
                          <i class="fas fa-trash"></i>
                        </span>
                    </button>
                </td>
            </tr>
            </tbody>
        </table>

        <p class="has-text-centered" v-if="repos.count == 0">Nothing to show</p>
        <pagination v-model="paginationSettings" v-if="repos.count > 0" />
    </div>

    <div class="modal is-active code" v-bind:class=" { 'hidden': !editScript } ">
        <div class="modal-background"></div>
        <div class="modal-content">
            <div class="box" v-click-outside="closeEdit">
                <div class="columns" v-if="editScript">
                    <div class="column">
                        <h1 class="title is-3">Edit REPO</h1>
                        <h4 class="subtitle is-6" v-if="mode == 'edit'">Source: {{editScript.descriptor.src || "not defined"}}</h4>
                        <div class="field" v-if="mode == 'new'">

                            <div class="field is-horizontal">
                              <div class="field-label is-normal">
                                <label class="label">Name:</label>
                              </div>
                              <div class="field-body">
                                <div class="field">
                                  <div class="control is-expanded has-icons-right">
                                    <input class="input" type="text" v-model="newName" placeholder="Name of the new REPO file">
                                    <span class="icon is-small is-right"><i class="fas fa-plus"></i></span>
                                  </div>
                                </div>
                              </div>
                            </div>

                            <div class="field is-horizontal">
                              <div class="field-label">
                                <label class="label">Mimetype</label>
                              </div>
                              <div class="field-body">
                                <div class="field is-narrow">
                                  <div class="control">
                                    <label v-for="mime in mimeTypes" class="radio">
                                        <input
                                            type="radio"
                                            name="groovy"
                                            :checked="mimeType == mime"
                                            style="margin-right: 0.25rem"
                                            v-on:change="setMimeType(mime)">{{mime}}</label>
                                  </div>
                                </div>
                              </div>
                            </div>
                        </div>

                    </div>
                    <div class="column is-one-fifth">
                        <div class="buttons has-addons is-right">
                            <button class="button is-success" @click="saveSource()" v-bind:class=" { 'is-loading': sending }" :disabled="!canSave()">
                                <span class="icon is-small" v-if="saved">
                                    <i class="fas fa-check"></i>
                                </span>
                                <span>Save</span>
                            </button>
                        </div>
                    </div>
                </div>

                <div id="configure-repos-editarea"></div>

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
            repos: {},
            //showWhoBroughtMe: null,
            filters: {
                from: 0,
                to: 5,
                name: '',
                src: '',
                entry: ''
            },

            // Determine the mode (edit, new)
            mode: 'edit',

            // Add new REPO
            newName: '',

            // Code editing
            codeMirror: null,
            opened: false,
            editScript: null,
            edited: false,
            sending: false,
            saved: false,
            errorCount: 0,
            errors: [],
            mimeType: "text/x-groovy",
            mimeTypes: [
                "text/x-groovy",
                "text/x-yaml"
            ]
        }
    },
    computed: {
        paginationSettings: {
            get() {
                var vm = this
                return {
                    refresh: function(from) {
                        vm.filters.from = from
                        vm.searchRepo()
                    },
                    from: vm.filters.from,
                    step: vm.filters.to,
                    total: vm.repos.count
                }
            }
        }
    },
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            vm.repos.descriptors.forEach(function(repo) {
                filtered.push(repo)
            })

            return filtered.sort(compareValues('name'))
        },
        searchRepo: function(fromFilter) {
            var vm = this

            if (fromFilter)
                vm.filters.from = 0

            axios.post(vm.value.api.links.repos.search, vm.filters).then(response => {
                vm.repos = response.data
            })
        },
        whenLastEdit: function(repo) {
            return TimeAgo.inWords(repo.script.lastEdit)
        },
        openAdd: function() {
            var vm = this

            vm.codeMirror.setValue('')
            vm.codeMirror.refresh()

            vm.editScript = {
                links: {
                    get save() {
                        return vm.value.api.links.repos.add + "?name=" + vm.newName + "&mimeType=" + vm.mimeType
                    }
                }
            }

            vm.edited = false
            vm.mode = 'new'
        },
        openEdit: function(repo) {
            var vm = this

            axios.get(repo.links.default).then(response => {
                var latestRepo = response.data

                vm.codeMirror.setValue(latestRepo.script.text)
                vm.codeMirror.refresh()

                vm.editScript = latestRepo
                vm.setMimeType(latestRepo.script.mimeType)

                vm.edited = false
                vm.mode = 'edit'

            })
        },
        canSave: function() {
            var vm = this

            if (!vm.edited)
                return false

            if (vm.mode == 'new' && vm.newName.length < 3)
                return false

            return true
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

                    if (vm.mode == 'edit')
                        vm.$bus.$emit('toast', `success:Saved <strong>${vm.editScript.descriptor.name}</strong> successfully!`)

                    if (vm.mode == 'new')
                        vm.$bus.$emit('toast', `success:Saved <strong>${vm.newName}</strong> successfully!`)

                    vm.searchRepo()
                }
            })
            .catch(err => {
                if (vm.mode == 'edit')
                    vm.$bus.$emit('toast', `success:Failed <strong>to save ${vm.editScript.descriptor.name}</strong>!`)

                if (vm.mode == 'new')
                    vm.$bus.$emit('toast', `success:Failed <strong>to save ${vm.newName}</strong>!`)
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
        },
        removeREPO: function(repo) {
            var vm = this

            axios.post(repo.links.remove).then(response => {
                vm.$bus.$emit('toast', `warn:Removed <strong>${repo.descriptor.name}</strong> successfully!`)
                vm.searchRepo()
            })
            .catch(err => {
                vm.$bus.$emit('toast', `error:Failed <strong>to remove ${repo.descriptor.name}</strong>!`)
            })
        },
        setMimeType: function(mimeType) {
            var vm = this

            vm.mimeType = mimeType
            vm.codeMirror.setOption("mode", mimeType)
        }
    },
    mounted: function() {
        var vm = this

        var codeMirror = CodeMirror(function(elt) {
            var element = document.querySelector('#configure-repos-editarea')
            element.appendChild(elt)
        }, {
            autoRefresh: true,
            lineNumbers: true,
            matchBrackets: true
        })

        codeMirror.on("change",function(cm,change){
            vm.saved = false
            vm.edited = true
        })

        codeMirror.setSize(null, 640)

        vm.codeMirror = codeMirror
        vm.searchRepo()
    }
})