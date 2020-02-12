Vue.component('configure', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2">
            <tab-tiles v-model="tabTilesSettings" />
        </div>

        <div class="column" v-if="currentTab.template">
            <component v-bind:is="currentTab.template" v-model="value" />
        </div>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentTab: 'parameters'
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'Params', description: 'Configure SCM parameters for each SELECTED INVs', template: 'configure-parameters'},
                        { label: 'Sources', description: 'Configure SCM scripts', template: 'configure-scms'}
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()
                    }
                }
            }
        }
    },
    methods: {
        completedCount: function() {
            var vm = this

            if (vm.value.selectedScms.descriptors.length == 0)
                return 0

            var count = 0

            vm.value.selectedScms.descriptors.forEach(function(scm) {
                if (!scm.completed) return

                count++
            })

            return count
        }
    }
})

Vue.component('configure-parameters', {
    template: `
<div>
    <div v-if="!areSCMsAvailable()">
        <div class="content">
            <p class="has-text-warning has-text-centered title is-4">Nothing is staged for now.</p>
            <p>Have you considered the following?</p>
            <ul>
                <li>Staging occurs in the <strong>choose</strong> step</li>
                <li>You may stage SCMs individually.</li>
                <li>You may also stage INVs by selecting <i>broadcast statement</i>.</li>
                <li>Composer determines which SCM is associated to each <i>broadcast statement</i> based on the <strong>run.txt</strong> file</li>
            </ul>
        </div>
    </div>
    <div v-else>
        <div class="field is-grouped is-grouped-right">
            <div class="field">
                <button @click="toggleHideOnComplete()" v-bind:class="{ 'is-link': filters.hideOnComplete}" class="button breath">
                    Hide when completed
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
                            <a @click="applyDefaultToAll()" class="dropdown-item">
                                Apply default to all
                            </a>
                        </div>
                        <div class="dropdown-content">
                            <a @click="resetAll()" class="dropdown-item">
                                Reset all
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <hr />

        <div v-if="value.selectedScms.total == 0" class="container">
            <p class="has-text-centered">Nothing to show</p>
        </div>
        <div class="columns is-multiline" v-else>
            <div class="column is-one-quarter" v-for="scm in value.selectedScms.descriptors" >
                <div class="card">
                    <div class="card-content">
                        <p class="title is-5">
                            {{scm.name}}
                            <span class="tag is-danger" v-show="scm.requiredNotCompletedCount > 0">{{scm.requiredNotCompletedCount}} parameter(s) required</span>
                            <span class="icon is-medium" v-if="scm.loading">
                                <i class="fas fa-spinner fa-pulse"></i>
                            </span>
                        </p>
                        <p class="subtitle is-6" style="color: lightgrey">
                            <span  v-if="scm.saved">Last edit: {{getRelativeTimestamp(scm)}}</span>
                            <span v-else>never saved</span>
                        </p>
                        <div class="content">
                            <ul>
                                <li>Has <span class="has-text-weight-bold">{{scm.parameters.length}}</span> parameter(s)</li>
                                <li>Has <span class="has-text-weight-bold">{{scm.requiredCount}}</span> required parameter(s)</li>
                                <li>
                                    <span v-if="scm.completed">Has <span class="has-text-weight-bold has-text-success">answered</span> all of its parameters</span>
                                    <span v-else>Has <span class="has-text-weight-bold has-text-warning">not answered</span> all of its parameters</span>
                                 </li>
                            </ul>
                        </div>
                    </div>

                    <footer class="card-footer">
                        <p class="card-footer-item is-paddingless">
                            <button class="button is-fullwidth is-link" @click.stop="editParameters(scm)" :disabled="scm.parameters.length == 0">Configure</button>
                        </p>
                        <p class="card-footer-item is-paddingless">
                            <button class="button is-fullwidth" @click="resetParameters(scm)" :disabled="scm.parameters.length == 0">Reset</button>
                        </p>
                    </footer>
                </div>
            </div>
        </div>

        <pagination v-model="paginationSettings" style="margin-top: 2em" />

        <div class="modal is-active" v-if="currentScmParameter && currentScmParameter.loaded" >
            <div class="modal-background"></div>
            <div class="modal-content">
                <div class="box" v-click-outside="close">
                    <h1 class="title is-5">Parameter(s) of: {{currentScmParameter.name}} </h1>
                    <div v-for="(parameter, index) in currentScmParameter.parameters" style="padding-bottom: 1em;">
                        <p>{{index +1 }}. {{parameter.name}}</p>
                        <p class="help">
                            Usage: {{parameter.usage}}.
                            Default value:
                            <a v-if="parameter.defaultValue" @click="setDefault(parameter)">{{parameter.defaultValue}}</a>
                            <span v-else>(not defined)</span>
                        </p>
                        <div class="field has-addons">
                            <div class="control">
                                <div v-if="parameter.values.length > 0">
                                    <div class="select" v-if="!areValuesUnavailable(parameter)" style="max-width: 300px;">
                                        <select v-model="parameter.value" @change="parameter.changed = true">
                                            <option value="" disabled hidden selected>Select value</option>
                                            <option v-for="value in parameter.values">{{value}}</option>
                                        </select>
                                    </div>
                                    <div class="field" v-else>
                                        <input class="input" type="text" value="No match found" disabled></input>
                                    </div>
                                </div>

                                <div class="field" v-if="parameter.values.length == 0">
                                    <input class="input" type="text" value="No values available" disabled></input>
                                </div>
                            </div>

                            <p class="control is-expanded">
                                <input class="input" type="text" placeholder="Value" v-model="parameter.value" @input="parameter.changed = true">
                            </p>
                            <button class="control button is-success" v-if="parameter.saved && !parameter.changed" v-bind:class=" { 'is-loading': parameter.sending }" :disabled="true">
                                <span>Saved</span>
                                <span class="icon is-small">
                                    <i class="fas fa-check"></i>
                                </span>
                            </button>
                        </div>

                    </div>

                    <footer class="modal-card-foot">
                      <button class="button is-success" :disabled="!hasAnyChanged(currentScmParameter)" @click="saveParameters(currentScmParameter)">Save all</button>
                      <button class="button" @click="resetParameters(currentScmParameter)">Reset all</button>
                    </footer>

                </div>
            </div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'find',
            currentScmParameter: null,
            filters: {
                staged: true,
                selected: true,
                to: 20,
                from: 0,
                hideOnComplete: true
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
                        vm.fetchScms()
                    },
                    from: vm.filters.from,
                    step: vm.filters.to,
                    total: vm.value.selectedScms.total
                }
            }
        }
    },
    methods: {
        areSCMsAvailable: function() {
            var vm = this

            if (vm.value.selectedScms.selected == undefined)
                return false

            if (vm.value.selectedScms.staged == undefined)
                return false

            return (vm.value.selectedScms.selected + vm.value.selectedScms.staged) > 0
        },
        fetchScms: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.search, vm.filters).then(response => {
                vm.value.selectedScms = response.data
                vm.value.selectedScms.descriptors.sort(compareValues('name'))

                // Calculate parameters metrics
                vm.value.selectedScms.descriptors.forEach(function(scm) {
                    scm.requiredCount = 0
                    scm.requiredNotCompletedCount = 0
                    scm.completedCount = 0

                    scm.parameters.forEach(function(parameter) {
                        if (parameter.required) {
                            scm.requiredCount++

                            if (parameter.value == undefined)
                                scm.requiredNotCompletedCount++
                        }

                        if (parameter.value != undefined)
                            scm.completedCount++
                    })
                })
            })
        },
        toggleHideOnComplete: function() {
            var vm = this

            vm.filters.hideOnComplete = !vm.filters.hideOnComplete
            vm.fetchScms()
        },
        applyDefaultToAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.applyDefaultAll).then(response => {
                vm.fetchScms()
           })
        },
        resetAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.resetAll).then(response => {
                vm.fetchScms()
            })
        },
        getRelativeTimestamp: function(scmParameters) {
            var vm = this

            return TimeAgo.inWords(scmParameters.lastModified)
        },
        editParameters: function(scmParameters) {

            var vm = this

            if (scmParameters.parameters.length == 0)
                return

            vm.currentScmParameter = scmParameters

            if (scmParameters.loaded)
                return

            scmParameters.loading = true

            axios.get(scmParameters.links.parameters).then(response => {

                scmParameters.parameters.forEach(function(parameter) {
                    parameter.values = response.data[parameter.name]

                    if (!parameter.value)
                        parameter.value = ""
                })

                scmParameters.changed = false
                scmParameters.loaded = true
                scmParameters.loading = false

                vm.$forceUpdate()
            })
            .catch(response => {
                console.log(response.data)
            })
        },
        areValuesUnavailable: function(scmParameters) {
            if (scmParameters.value == undefined)
                return false

            if (scmParameters.value == null)
                return false

            if (scmParameters.value === '')
                return false

            return scmParameters.values.indexOf(scmParameters.value) < 0
        },
        hasAnyChanged: function(scmParameters) {
            return scmParameters.parameters.filter(function(parameter) {
                return parameter.changed
            }).length > 0
        },
        saveParameters: function(scm) {
            var vm = this

            scm.parameters.forEach(function(parameter) {
                if (!parameter.changed)
                    return

                vm.saveParameter(parameter)
            })
        },
        resetParameters: function(scm) {
            var vm = this

            scm.parameters.forEach(function(parameter) {

                if (parameter.value == null ||
                    parameter.value == undefined ||
                    parameter.value == '')
                    return

                parameter.value = ''

                vm.saveParameter(parameter)
            })
        },
        setDefault: function(parameter) {
            parameter.value = parameter.defaultValue
            parameter.changed = true
        },
        saveParameter: function(parameter) {
            var vm = this

            if (parameter.sending)
                return

            parameter.sending = true

            axios.post(parameter.links.save,{
                parameterValue: parameter.value
            }).then(response => {

                parameter.sending = false
                parameter.saved = true
                parameter.changed = false

                vm.$forceUpdate()
            })
        },
        close: function() {
            var vm = this

            axios.get(vm.currentScmParameter.links.default).then(response => {

                vm.currentScmParameter.saved = response.data.saved
                vm.currentScmParameter.lastModified = response.data.lastModified
                vm.currentScmParameter.completed = response.data.completed
                vm.currentScmParameter = null

                vm.fetchScms()
            })
        }
    },
    created: function() {
        var vm = this

        vm.fetchScms()
    }
})

Vue.component('configure-scms', {
    template: `
<div>
    <div v-if="value.scms.total == undefined">
        Nothing selected yet...
    </div>
    <table class="table is-striped is-narrow is-hoverable is-fullwidth" v-else>
        <thead>
        <tr class="field">
            <th ><input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="searchScm(true)"></th>
            <th><input class="input" type="text" v-model="filters.src" placeholder="Source" @keyup="searchScm(true)"></th>
            <th><input class="input" type="text" v-model="filters.entry" placeholder="Entry" @keyup="searchScm(true)"></th>
            <th style="width: 8%">Timeout</th>
            <th style="width: 8%">Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="scm in filter()">
            <td><span>{{scm.name}}</span><br/><span style="color: lightgrey">Last edit: {{getRelativeTimestamp(scm)}}</span></td>
            <td>{{scm.descriptor.src}}</td>
            <td><p v-for="entry in scm.descriptor.entry">{{entry}}</p></td>
            <td>{{scm.descriptor.timeout}}</td>
            <td><button class="button is-link" @click.stop="openEdit(scm)">Edit</button></td>
        </tr>
        </tbody>
    </table>

    <pagination v-model="paginationSettings" />

    <div class="modal is-active" v-bind:class=" { 'code-hidden': !editScript } ">
        <div class="modal-background"></div>
        <div class="modal-content code-edit-modal">
            <div class="box" v-click-outside="closeEdit">
                <div class="columns" v-if="editScript">
                    <div class="column">
                        <h1 class="title is-3">Edit</h1>
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

                <div id="editArea"></div>

                <div v-if="errorCount > 0">
                    <p>Compilation error(s) caught:</p>
                    <p class="has-text-danger" v-for="error in errors">{{error}}</p>
                </div>
            </div>
        </div>
    </div>

    <div class="modal is-active" v-if="showWhoBroughtMe">
        <div class="modal-background"></div>
        <div class="modal-content">
            <div class="box" v-click-outside="closeWhoBroughtMe">
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
                    <tr v-for="other in whoBroughtMe(showWhoBroughtMe)">
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
            // Table
            count: this.value.scms.length,
            showWhoBroughtMe: null,
            filters: {
                selected: true,
                from: 0,
                step: 20,
                name: '',
                src: '',
                entry: ''
            },

            // Code editing
            codeMirror: null,
            editScript: '',
            edited: false,
            sending: false,
            saved: false,
            errorCount: 0,
            errors: []
        }
    },
    mounted: function() {
        var element = document.querySelector('#editArea')

        if (element.childNodes.length > 0) {
            element.removeChild(element.childNodes[0])
        }

        var codeMirror = CodeMirror(function(elt) {
          element.appendChild(elt)
        }, {
            autoRefresh: true,
            lineNumbers: true,
            matchBrackets: true,
            mode: "text/x-groovy"
        })

        codeMirror.setSize(null, 640)

        this.codeMirror = codeMirror
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
        getRelativeTimestamp: function(scm) {
            return TimeAgo.inWords(scm.script.lastEdit)
        },
        openEdit: function(scm) {
            var vm = this

            axios.get(scm.links.default).then(response => {
                var latestScm = response.data

                vm.codeMirror.setValue(latestScm.script.text)
                vm.codeMirror.refresh()

                vm.editScript = latestScm

                vm.codeMirror.on("change",function(cm,change){
                    vm.saved = false
                    vm.edited = true
                })
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
        },
        closeWhoBroughtMe: function() {
            this.showWhoBroughtMe = ''
        }
    },
    created: function() {
        this.searchScm()
    }
})