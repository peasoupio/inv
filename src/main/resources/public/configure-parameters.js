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
                            <span v-bind:class="{ 'has-text-danger': scm.errors.length > 0 }">{{scm.name}}</span>
                            <span class="icon is-medium" v-if="scm.loading">
                                <i class="fas fa-spinner fa-pulse"></i>
                            </span>
                            <p>
                                <span @mouseover="scm.showErrors=true" @mouseleave="scm.showErrors=false" class="tag is-danger" v-show="scm.errors.length > 0">{{scm.errors.length}} error(s) caught</span>
                                <span class="tag is-warning" v-show="scm.requiredNotCompletedCount > 0">{{scm.requiredNotCompletedCount}} parameter(s) required</span>
                            </p>
                        </p>
                        <div class="notification is-primary content" v-show="scm.showErrors" style="position: absolute; z-index: 10">
                            <ul>
                                <li v-for="error in scm.errors">{{error.message}}, {{whenError(error)}}.</li>
                            </ul>
                        </div>
                        <p class="subtitle is-6" style="color: lightgrey">
                            <span  v-if="scm.saved">Last edit: {{whenLastSaved(scm)}}</span>
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
                    total: vm.value.selectedScms.count
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
                    // dependency metrics
                    scm.requiredCount = 0
                    scm.requiredNotCompletedCount = 0
                    scm.completedCount = 0

                    // errors stack
                    scm.errors = []
                    vm.$set(scm, 'showErrors', false)

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
        whenLastSaved: function(scmParameters) {
            var vm = this

            return TimeAgo.inWords(scmParameters.lastModified)
        },
        whenError: function(error) {
            var vm = this

            return TimeAgo.inWords(error.when)
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
            .catch(error  => {

                scmParameters.changed = false
                scmParameters.loaded = false
                scmParameters.loading = false

                scmParameters.errors.push(error.response.data)

                vm.$forceUpdate()
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