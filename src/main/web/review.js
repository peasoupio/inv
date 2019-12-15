Vue.component('review', {
    template: `
<div>
    <p class="title is-1">Choose: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='parameter' }"><a v-on:click="activeTab='parameter'">Parameters</a></li>
        </ul>
    </div>

    <review-parameter v-model="value" v-if="activeTab=='parameter'"></choose-find>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'parameter'
        }
    },
    methods: {

    }
})

Vue.component('review-parameter', {
    template: `
<div>
    <div v-for="scm in filter()" class="box">
        <p class="subtitle is-5">
            {{scm.name}}
            <span class="icon is-medium" v-if="scm.loading">
                <i class="fas fa-spinner fa-pulse"></i>
            </span>
        </p>
        <div v-if="scm.hasParameters">
            <div class="field is-horizontal" v-for="parameter in scm.parameters">
                <div class="field-label is-normal">
                    <label class="label">{{parameter.name}}</label>
                </div>
                <div class="field-body">
                    <div class="field is-expanded">
                        <div class="field has-addons">
                            <div class="control">
                                <div v-if="parameter.values.length > 0">
                                    <div class="select" v-if="!areValuesUnavailable(parameter)">
                                        <select v-model="parameter.value">
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
                                <input class="input" type="text" placeholder="Value" v-model="parameter.value">
                            </p>
                            <button class="control button is-success" v-on:click="saveParameter(parameter)" v-bind:class=" { 'is-loading': parameter.sending }">
                                <span class="icon is-small" v-if="parameter.savedOnce">
                                    <i class="fas fa-check"></i>
                                </span>
                                <span>Save</span>
                            </button>
                        </div>
                        <p class="help">
                            {{parameter.usage}}.
                            Default value:
                            <a v-if="parameter.defaultValue !== ''" v-on:click="parameter.value = parameter.defaultValue">{{parameter.defaultValue}}</a>
                            <span v-else>(not defined)</span>
                        </p>
                    </div>
                </div>
            </div>
        </div>
        <div v-else>
            <p class="subtitle is-6 has-text-centered">Has no parameters. Yay!</p>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            scmParameters: [],
            activeTab: 'find'
        }
    },
    methods: {
        filter: function() {
            var vm = this

            return vm.scmParameters.filter(function(scm) {
                return vm.whoBroughtMe(scm).length > 0
            })

        },
        areValuesUnavailable: function(parameter) {
            return parameter.value !== '' && parameter.values.indexOf(parameter.value) < 0
        },
        saveParameter: function(parameter) {
            if (parameter.sending)
                return

            parameter.sending = true

            axios.post(parameter.links.save,{
                parameterValue: parameter.value
            }).then(response => {
                parameter.sending = false
                parameter.savedOnce = true
            })
        },
        whoBroughtMe: function(scm) {
            var vm = this

            if (!scm)
                return

            var whoBroughtMe = []

            vm.value.availables.forEach(function(chosen) {

                if (!chosen.chosen && !chosen.broughtBySomeone)
                    return

                if (chosen.scm != scm.name)
                    return

                whoBroughtMe.push(chosen)

                chosen.required.forEach(function(required) {
                    if (whoBroughtMe.indexOf(chosen) > -1)
                        return

                    whoBroughtMe.push(required)
                })
            })

            return whoBroughtMe.sort(compareValues('owner'))
        }
    },
    created: function() {
        var vm = this

        var registry = this.value.scms.registry

        for(var key in registry) {
            if (!registry.hasOwnProperty(key)) continue

            var scm = registry[key]

            var scmParameters = {
                hasParameters: scm.descriptor.hasParameters,
                scm: scm,
                name: key,
                loading: false,
                parameters: []
            }

            vm.scmParameters.push(scmParameters)

            if (!scm.descriptor.hasParameters) {
                continue
            }

            scmParameters.loading = true

            axios.get(scm.links.parameters).then(response => {

                var owner = response.data.owner

                var myScmParameters = vm.scmParameters.filter(function(other) {
                    return other.name == owner
                })[0]

                response.data.parameters.forEach(function(parameter) {
                    parameter.open = false

                    if (parameter.value == null)
                        parameter.value = parameter.defaultValue

                    parameter.sending = false
                    parameter.savedOnce = false

                    myScmParameters.parameters.push(parameter)
                })

                myScmParameters.loading = false
            })

            vm.scmParameters.sort(compareValues('name'))
        }
    }
})