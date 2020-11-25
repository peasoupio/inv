

Vue.component('configure-parameters', {
    template: `
<div>
    <div v-if="!areREPOsAvailable">
        <div class="content">
            <p class="has-text-warning has-text-centered title is-4">Nothing is staged for now.</p>
            <p>Have you considered the following?</p>
            <ul>
                <li>Staging occurs in the <strong>choose</strong> step</li>
                <li>You may stage REPOs individually.</li>
                <li>You may also stage INVs by selecting <i>broadcast statement</i>.</li>
                <li>Composer determines which REPO is associated to each <i>broadcast statement</i> based on the <strong>run.txt</strong> file</li>
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

        <div class="modal is-active" v-if="currentRepoParameter && currentRepoParameter.loaded" >
            <div class="modal-background"></div>
            <div class="modal-content">
                <div class="box" v-click-outside="close">
                    <h1 class="title is-4">Parameter(s) of: {{currentRepoParameter.name}} </h1>
                    <div v-for="(parameter, index) in currentRepoParameter.parameters" style="padding-bottom: 1em;">
                        <p>
                            <span class="title is-5">{{index +1 }}. {{parameter.name}}</span>
                            <span class="title is-6" v-if="parameter.values.length">(has {{parameter.values.length}} value(s) available)</span>
                        </p>
                        <p class="help">
                            Usage: {{parameter.usage}}.
                            Default value:
                            <a v-if="parameter.defaultValue" @click="setDefault(parameter)">{{parameter.defaultValue}}</a>
                            <span v-else>(not defined)</span>
                        </p>
                        <div class="field has-addons">
                            <div class="control is-expanded">
                                <input class="input" :list="'list-' + parameter.name" v-model="parameter.value" @input="parameter.changed=true" placeholder="Define new value"></input>
                                <datalist :id="'list-' + parameter.name">
                                    <option v-for="value in parameter.values">{{value}}</option>
                                </datalist>
                            </div>

                            <button class="control button is-success" v-if="parameter.saved && !parameter.changed" v-bind:class=" { 'is-loading': parameter.sending }" :disabled="true">
                                <span>Saved</span>
                                <span class="icon is-small">
                                    <i class="fas fa-check"></i>
                                </span>
                            </button>
                        </div>
                    </div>

                    <footer class="modal-card-foot">
                      <button class="button is-success" :disabled="!hasAnyChanged(currentRepoParameter)" @click="saveParameters(currentRepoParameter)">Save all</button>
                      <button class="button" @click="resetParameters(currentRepoParameter)">Reset all</button>
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
            areREPOsAvailable: false,
            filters: {
                hideOnComplete: true
            },
            updateIndex: 0,
            currentRepoParameter: null
        }
    },
    computed: {
        selectedSettings: {
            get() {
                var vm = this
                return {
                    api: vm.value.api,
                    edit: function(repo) {
                        vm.currentRepoParameter = repo
                    },
                    reset: function(repo) {
                        vm.resetParameters(repo)
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
                    edit: function(repo) {
                        vm.currentRepoParameter = repo
                        vm.$forceUpdate()
                    },
                    reset: function(repo) {
                        vm.resetParameters(repo)
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

            vm.selectedSettings.filters.hideOnComplete = vm.filters.hideOnComplete
            vm.requiredSettings.filters.hideOnComplete = vm.filters.hideOnComplete

            vm.updateIndex++
        },
        applyDefaultToAll: function() {
            var vm = this

            axios.post(vm.value.api.links.repos.applyDefaultAll).then(response => {
                vm.updateIndex++

                vm.$bus.$emit('toast', `warn:Applied <strong>all defaults parameters</strong> successfully!`)
           })
        },
        resetAll: function() {
            var vm = this

            axios.post(vm.value.api.links.repos.resetAll).then(response => {
                vm.updateIndex++

                vm.$bus.$emit('toast', `warn:Reset <strong>all parameters</strong> successfully!`)
            })
        },
        areValuesUnavailable: function(repoParameters) {
            if (repoParameters.value == undefined)
                return false

            if (repoParameters.value == null)
                return false

            if (repoParameters.value === '')
                return false

            return repoParameters.values.indexOf(repoParameters.value) < 0
        },
        hasAnyChanged: function(repoParameters) {
            return repoParameters.parameters.filter(function(parameter) {
                return parameter.changed
            }).length > 0
        },
        saveParameters: function(repo) {
            var vm = this

            repo.parameters.forEach(function(parameter) {
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
        resetParameters: function(repo) {
            var vm = this

            repo.parameters.forEach(function(parameter) {

                if (parameter.value == null ||
                    parameter.value == undefined ||
                    parameter.value == '')
                    return

                parameter.value = ''

                vm.saveParameter(parameter)
            })

            repo.updateIndex++
            vm.$bus.$emit('toast', `warn:Reset <strong>parameters</strong> successfully!`)
        },
        saveParameter: function(parameter) {
            if (parameter.sending)
                return

            parameter.sending = true

            axios.post(parameter.links.save,{
                parameterValue: parameter.value
            }).then(response => {

                parameter.sending = false
                parameter.saved = true
                parameter.changed = false
            })
        },
        close: function() {
            var vm = this

            axios.get(vm.currentRepoParameter.links.default).then(response => {

                vm.currentRepoParameter.saved = response.data.saved
                vm.currentRepoParameter.lastModified = response.data.lastModified
                vm.currentRepoParameter.completed = response.data.completed
                vm.currentRepoParameter.updateIndex++
                vm.currentRepoParameter = null
            })
        }
    },
    mounted: function() {
        var vm = this

        // Check if any repo is selected or staged
        axios.get(vm.value.api.links.repos.metadata).then(response => {
            vm.areREPOsAvailable = (response.data.selected + response.data.staged) > 0
        })
    }
})


Vue.component('configure-parameters-carousel', {
    template: `
<div>
    <p class="title is-3">
        {{value.title}} ({{repos.count}}/{{repos.total}})
    </p>

    <div class="field">
        <p class="control is-expanded has-icons-right">
            <input class="input" type="text" v-model="filters.name" placeholder="Name" @keyup="filterRepos()">
            <span class="icon is-small is-right"><i class="fas fa-search"></i></span>
        </p>
    </div>

    <div v-if="repos.count == 0" class="container">
        <p class="has-text-centered">Nothing to show</p>
    </div>
    <div class="columns is-multiline" style="min-height: 300px;" v-else>
        <div class="column is-one-quarter" v-for="repo in repos.descriptors">
            <div class="card" :key="repo.updateIndex && getStats(repo)">
                <div class="card-content">
                    <p class="title is-5">
                        <span v-bind:class="{ 'has-text-danger': repo.errors.length > 0, 'has-text-success': repo.completed }">
                            {{repo.name}}
                        </span>

                        <span class="icon is-medium" v-if="repo.loading">
                            <i class="fas fa-spinner fa-pulse"></i>
                        </span>
                    </p>
                    <p class="subtitle is-6" style="color: lightgrey">
                        <span  v-if="repo.saved">Last edit: {{whenLastSaved(repo)}}</span>
                        <span v-else>never saved</span>
                    </p>

                    <p class="has-text-centered">
                        <span class="tag is-danger"
                            style="cursor: pointer"
                            @mouseover="repo.showErrors=true"
                            @mouseleave="repo.showErrors=false"
                            v-show="repo.errors.length > 0">{{repo.errors.length}} error(s) caught</span>

                        <span class="tag"
                            style="cursor: pointer"
                            @mouseover="repo.showDetails=true"
                            @mouseleave="repo.showDetails=false"
                            v-bind:class="{ 'is-warning': repo.requiredNotCompletedCount > 0 }">{{repo.requiredNotCompletedCount}} parameter(s) required</span>
                    </p>
                </div>

                <footer class="card-footer">
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth" @click="resetParameters(repo)" :disabled="repo.parameters.length == 0">Reset</button>
                    </p>
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth is-link" @click.stop="editParameters(repo)" :disabled="repo.parameters.length == 0">Configure</button>
                    </p>
                </footer>

                <div class="notification is-primary content" v-if="repo.showDetails" style="position: absolute; z-index: 10">
                    <ul>
                        <li>Has <span class="has-text-weight-bold">{{repo.parameters.length}}</span> parameter(s)</li>
                        <li>Has <span class="has-text-weight-bold">{{repo.requiredCount}}</span> required parameter(s)</li>
                        <li>Has answered <span class="has-text-weight-bold">{{repocompletedCount}}</span> parameter(s)</li>
                        <li>
                            <span v-if="repo.completed">Has <span class="has-text-weight-bold has-text-success">answered</span> all of its parameters</span>
                            <span v-else>Has <span class="has-text-weight-bold has-text-warning">not answered</span> all of its parameters</span>
                         </li>
                    </ul>
                </div>

                <div class="notification is-danger content" v-show="repo.showErrors" style="position: absolute; z-index: 10">
                    <ul>
                        <li v-for="error in repo.errors">{{error.message}}, {{whenError(error)}}.</li>
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
            repos: [],
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
                        vm.fetchRepos()
                    },
                    from: vm.filters.from,
                    step: vm.filters.to,
                    total: vm.repos.count
                }
            }
        }
    },
    methods: {
        filterRepos: function() {
            var vm = this

            vm.filters.from = 0
            vm.fetchRepos()
        },
        fetchRepos: function() {
            var vm = this

            vm.loading = true

            axios.post(vm.value.api.links.repos.search, vm.filters).then(response => {
                vm.repos = response.data
                vm.repos.descriptors.sort(compareValues('name'))

                // Calculate parameters metrics
                vm.repos.descriptors.forEach(function(repo) {
                    // dependency metrics
                    repo.requiredCount = 0
                    repo.requiredNotCompletedCount = 0
                    repo.completedCount = 0

                    // errors stack
                    repo.errors = []
                    vm.$set(repo, 'showErrors', false)
                    vm.$set(repo, 'loading', false)
                    vm.$set(repo, 'showDetails', false)
                    vm.$set(repo, 'updateIndex', 0)

                    // Initialized stats
                    vm.getStats(repo)
                })

                vm.loading = false
            })
        },
        whenLastSaved: function(repoParameters) {
            return TimeAgo.inWords(repoParameters.lastModified)
        },
        whenError: function(error) {
            return TimeAgo.inWords(error.when)
        },
        getStats: function(repo) {
            repo.requiredCount = 0
            repo.requiredNotCompletedCount = 0
            repo.completedCount = 0

            repo.parameters.forEach(function(parameter) {
                var hasValue = parameter.value != null &&
                               parameter.value != undefined &&
                               parameter.value != ''

                if (parameter.required) {
                    repo.requiredCount++

                    if (!hasValue)
                        repo.requiredNotCompletedCount++
                }

                if (hasValue)
                    repo.completedCount++
            })

            return true
        },
        editParameters: function(repoParameters) {

            var vm = this

            if (repoParameters.parameters.length == 0)
                return

            if (repoParameters.loaded) {
                vm.value.edit(repoParameters)
                return
            }

            repoParameters.loading = true

            axios.get(repoParameters.links.parameters).then(response => {

                repoParameters.parameters.forEach(function(parameter) {
                    parameter.values = response.data[parameter.name]

                    if (!parameter.value)
                        parameter.value = ""
                })

                repoParameters.changed = false
                repoParameters.loaded = true
                repoParameters.loading = false

                vm.value.edit(repoParameters)
            })
            .catch(err => {

                repoParameters.changed = false
                repoParameters.loaded = false
                repoParameters.loading = false

                repoParameters.errors.push(error.response.data)
                vm.$forceUpdate()

                vm.$bus.$emit('toast', `error:Failed to <strong>edit parameters</strong>!`)
            })
        },
        resetParameters: function(repoParameters) {
            var vm = this
            vm.value.reset(repoParameters)
        }
    },
    created: function() {
        var vm = this

        vm.fetchRepos()
    }
})