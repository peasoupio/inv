

Vue.component('configure-parameters', {
    template: `
<div>
    <div v-if="!areSCMsAvailable">
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
        <configure-parameters-carousel v-model="selectedSettings" :key="updateIndex" />
        <hr />
        <configure-parameters-carousel v-model="requiredSettings" :key="updateIndex" />

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
            areSCMsAvailable: false,
            filters: {
                hideOnComplete: true
            },
            updateIndex: 0,
            currentScmParameter: null
        }
    },
    computed: {
        selectedSettings: {
            get() {
                var vm = this
                return {
                    api: vm.value.api,
                    edit: function(scm) {
                        vm.currentScmParameter = scm
                    },
                    reset: function(scm) {
                        vm.resetParameters(scm)
                    },
                    title: 'Added from the choosing options',
                    filters: {
                        selected: true,
                        required: false,
                        hideOnComplete: vm.filters.hideOnComplete
                    }

                }
            }
        },
        requiredSettings: {
            get() {
                var vm = this
                return {
                    api: vm.value.api,
                    edit: function(scm) {
                        vm.currentScmParameter = scm
                        vm.$forceUpdate()
                    },
                    reset: function(scm) {
                        vm.resetParameters(scm)
                    },
                    title: 'Added manually',
                    filters: {
                        selected: false,
                        required: true,
                        hideOnComplete: vm.filters.hideOnComplete
                    }
                }
            }
        }
    },
    methods: {
        toggleHideOnComplete: function() {
            var vm = this

            vm.filters.hideOnComplete = !vm.filters.hideOnComplete

            vm.updateIndex++
        },
        applyDefaultToAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.applyDefaultAll).then(response => {
                vm.updateIndex++

                vm.$bus.$emit('toast', `warn:Applied <strong>all defaults parameters</strong> successfully!`)
           })
        },
        resetAll: function() {
            var vm = this

            axios.post(vm.value.api.links.scms.resetAll).then(response => {
                vm.updateIndex++

                vm.$bus.$emit('toast', `warn:Reset <strong>all parameters</strong> successfully!`)
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

            vm.$bus.$emit('toast', `success:Saved <strong>parameters</strong> successfully!`)
        },
        setDefault: function(parameter) {
            parameter.value = parameter.defaultValue
            parameter.changed = true
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

            vm.$bus.$emit('toast', `warn:Reset <strong>parameters</strong> successfully!`)
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

                vm.updateIndex++
            })
        },
        close: function() {
            var vm = this

            axios.get(vm.currentScmParameter.links.default).then(response => {

                vm.currentScmParameter.saved = response.data.saved
                vm.currentScmParameter.lastModified = response.data.lastModified
                vm.currentScmParameter.completed = response.data.completed
                vm.currentScmParameter = null

                vm.updateIndex++
            })
        }
    },
    mounted: function() {
        var vm = this

        // Check if any scm is selected or staged
        axios.get(vm.value.api.links.scms.metadata).then(response => {
            vm.areSCMsAvailable = (response.data.selected + response.data.staged) > 0
        })
    }
})


Vue.component('configure-parameters-carousel', {
    template: `
<div>
    <p class="title is-3">
        {{value.title}} ({{scms.count}}/{{scms.total}})
    </p>

    <div class="field">
        <p class="control is-expanded has-icons-right">
            <input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="fetchScms()">
            <span class="icon is-small is-right"><i class="fas fa-search"></i></span>
        </p>
    </div>

    <div v-if="scms.count == 0" class="container">
        <p class="has-text-centered">Nothing to show</p>
    </div>
    <div class="columns is-multiline" v-else>
        <div class="column is-one-quarter" v-for="scm in scms.descriptors" >
            <div class="card">
                <div class="card-content">
                    <p class="title is-5">
                        <span v-bind:class="{ 'has-text-danger': scm.errors.length > 0, 'has-text-success': scm.completed }">
                            {{scm.name}}
                        </span>

                        <span class="icon is-medium" v-if="scm.loading">
                            <i class="fas fa-spinner fa-pulse"></i>
                        </span>
                    </p>
                    <p class="subtitle is-6" style="color: lightgrey">
                        <span  v-if="scm.saved">Last edit: {{whenLastSaved(scm)}}</span>
                        <span v-else>never saved</span>
                    </p>

                    <p class="has-text-centered">
                        <span class="tag is-danger"
                            style="cursor: pointer"
                            @mouseover="scm.showErrors=true"
                            @mouseleave="scm.showErrors=false"
                            v-show="scm.errors.length > 0">{{scm.errors.length}} error(s) caught</span>

                        <span class="tag"
                            style="cursor: pointer"
                            @mouseover="scm.showDetails=true"
                            @mouseleave="scm.showDetails=false"
                            v-bind:class="{ 'is-warning': scm.requiredNotCompletedCount > 0 }">{{scm.requiredNotCompletedCount}} parameter(s) required</span>
                    </p>
                </div>

                <footer class="card-footer">
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth" @click="resetParameters(scm)" :disabled="scm.parameters.length == 0">Reset</button>
                    </p>
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth is-link" @click.stop="editParameters(scm)" :disabled="scm.parameters.length == 0">Configure</button>
                    </p>
                </footer>

                <div class="notification is-primary content" v-if="scm.showDetails" style="position: absolute; z-index: 10">
                    <ul>
                        <li>Has <span class="has-text-weight-bold">{{scm.parameters.length}}</span> parameter(s)</li>
                        <li>Has <span class="has-text-weight-bold">{{scm.requiredCount}}</span> required parameter(s)</li>
                        <li>Has answered <span class="has-text-weight-bold">{{scm.completedCount}}</span> parameter(s)</li>
                        <li>
                            <span v-if="scm.completed">Has <span class="has-text-weight-bold has-text-success">answered</span> all of its parameters</span>
                            <span v-else>Has <span class="has-text-weight-bold has-text-warning">not answered</span> all of its parameters</span>
                         </li>
                    </ul>
                </div>

                <div class="notification is-danger content" v-show="scm.showErrors" style="position: absolute; z-index: 10">
                    <ul>
                        <li v-for="error in scm.errors">{{error.message}}, {{whenError(error)}}.</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>

    <pagination v-model="paginationSettings" style="margin-top: 2em" />
</div>
`,
    props: ['value'],
    data: function() {
        var vm = this

        return {
            loading: false,
            scms: [],
            total: 0,
            filters: {
                name: '',
                staged: vm.value.filters.required,
                selected: vm.value.filters.selected,
                to: 4,
                from: 0,
                hideOnComplete: vm.value.filters.hideOnComplete
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
                    total: vm.scms.count
                }
            }
        }
    },
    methods: {
        fetchScms: function() {
            var vm = this

            vm.loading = true

            axios.post(vm.value.api.links.scms.search, vm.filters).then(response => {
                vm.scms = response.data
                vm.scms.descriptors.sort(compareValues('name'))

                // Calculate parameters metrics
                vm.scms.descriptors.forEach(function(scm) {
                    // dependency metrics
                    scm.requiredCount = 0
                    scm.requiredNotCompletedCount = 0
                    scm.completedCount = 0

                    // errors stack
                    scm.errors = []
                    vm.$set(scm, 'showErrors', false)
                    vm.$set(scm, 'loading', false)
                    vm.$set(scm, 'showDetails', false)

                    scm.parameters.forEach(function(parameter) {
                        var hasValue = parameter.value != null &&
                                       parameter.value != undefined &&
                                       parameter.value != ''

                        if (parameter.required) {
                            scm.requiredCount++

                            if (!hasValue)
                                scm.requiredNotCompletedCount++
                        }

                        if (hasValue)
                            scm.completedCount++
                    })
                })

                vm.loading = false
            })
        },
        whenLastSaved: function(scmParameters) {
            return TimeAgo.inWords(scmParameters.lastModified)
        },
        whenError: function(error) {
            return TimeAgo.inWords(error.when)
        },
        editParameters: function(scmParameters) {

            var vm = this

            if (scmParameters.parameters.length == 0)
                return

            if (scmParameters.loaded) {
                vm.value.edit(scmParameters)
                return
            }

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

                vm.value.edit(scmParameters)
            })
            .catch(err => {

                scmParameters.changed = false
                scmParameters.loaded = false
                scmParameters.loading = false

                scmParameters.errors.push(error.response.data)
                vm.$forceUpdate()

                vm.$bus.$emit('toast', `error:Failed to <strong>edit parameters</strong>!`)
            })
        },
        resetParameters: function(scmParameters) {
            var vm = this
            vm.value.reset(scmParameters)
        }
    },
    created: function() {
        var vm = this

        vm.fetchScms()
    }
})