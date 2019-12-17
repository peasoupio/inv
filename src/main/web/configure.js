Vue.component('configure', {
    template: `
<div>

    <p class="title is-1">Configure: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='parameters' }"><a @click="activeTab='parameters'">Parameters</a></li>
            <li v-bind:class="{ 'is-active' : activeTab=='scms' }"><a @click="activeTab='scms'">Scms ({{Object.values(value.scms.registry).length}})</a></li>
        </ul>
    </div>

    <configure-parameters v-model="value" v-if="activeTab=='parameters'"></configure-parameters>
    <configure-scms v-model="value" v-if="activeTab=='scms'"></configure-scms>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'parameters'
        }
    },
    methods: {
    }
})

Vue.component('configure-parameters', {
    template: `
<div>
    <div class="columns is-multiline">
        <div class="column is-one-quarter" v-for="scm in filter()" >
            <div class="card">
                <header class="card-header">
                    <p class="card-header-title">
                        {{scm.name}}
                        <span class="icon is-medium" v-if="scm.loading">
                            <i class="fas fa-spinner fa-pulse"></i>
                        </span>
                    </p>
                </header>
                <div class="card-content">
                    <div class="content">
                        {{scm.scm.descriptor.src}}
                    </div>
                </div>

                <footer class="card-footer">
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth is-success" @click.stop="expand(scm)" :disabled="!scm.hasParameters">Configure</button>
                    </p>
                    <p class="card-footer-item is-paddingless">
                        <button class="button is-fullwidth" :disabled="!scm.hasParameters">Reset</button>
                    </p>
                </footer>
            </div>
        </div>

    <div class="modal is-active" v-if="currentScmParameter && currentScmParameter.loaded" >
        <div class="modal-background"></div>
        <div class="modal-content">
            <div class="box" v-click-outside="close">
                <div class="field is-horizontal" v-for="parameter in currentScmParameter.parameters">
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
                                    <input class="input" type="text" placeholder="Value" v-model="parameter.value" @input="parameter.changed = true">
                                </p>
                                <button class="control button is-success" v-if="parameter.saved && !parameter.changed" v-bind:class=" { 'is-loading': parameter.sending }" :disabled="true">
                                    <span>Saved</span>
                                    <span class="icon is-small">
                                        <i class="fas fa-check"></i>
                                    </span>
                                </button>
                            </div>
                            <p class="help">
                                {{parameter.usage}}.
                                Default value:
                                <a v-if="parameter.defaultValue !== ''" @click="setDefault(parameter)">{{parameter.defaultValue}}</a>
                                <span v-else>(not defined)</span>
                            </p>
                        </div>
                    </div>
                </div>

                <footer class="modal-card-foot">
                  <button class="button is-success" :disabled="!hasAnyChanged(currentScmParameter)" @click="saveAll(currentScmParameter)">Save all</button>
                  <button class="button" @click="resetAll(currentScmParameter)">Reset all</button>
                </footer>

            </div>
        </div>
    </div>
    <!--

    <div v-if="count == 0">
        No Scms chosen yet
    </div>
<p class="subtitle is-5">
            <button class="control button is-success" >

                <span>{{scm.name}}</span>
            </button>
        </p>

        <div v-else>
            <p class="subtitle is-6 has-text-centered">Has no parameters. Yay!</p>
        </div>

<div v-if="scm.hasParameters && scm.expanded">

</div>

    -->
</div>
`,
    props: ['value'],
    data: function() {
        return {
            count: 0,
            scmParameters: [],
            activeTab: 'find',
            currentScmParameter: null
        }
    },
    methods: {

        isBroughtByAny: function(owner) {
            var vm = this

            var any = false

            vm.value.invs.nodes.forEach(function(inv) {
                if (!inv.chosen && inv.broughtBySomeone.length == 0)
                    return

                if (inv.scm != owner.name)
                    return

                any = true
            })

            return any
        },
        filter: function() {
            var vm = this

            var results = vm.scmParameters.filter(function(scm) {
                return vm.isBroughtByAny(scm)
            })

            vm.count = results.length

            return results

        },
        areValuesUnavailable: function(parameter) {
            return parameter.value !== '' && parameter.values.indexOf(parameter.value) < 0
        },
        expand: function(scmParameters) {

            var vm = this

            if (!scmParameters.hasParameters)
                return

            vm.currentScmParameter = scmParameters

            if (scmParameters.loaded)
                return

            scmParameters.loading = true

            axios.get(scmParameters.scm.links.parameters).then(response => {

                var owner = response.data.owner

                response.data.parameters.forEach(function(parameter) {
                    parameter.open = false

                    if (parameter.value == null)
                        parameter.value = parameter.defaultValue

                    parameter.sending = false
                    parameter.saved = false
                    parameter.changed = false

                    scmParameters.parameters.push(parameter)
                })

                scmParameters.loaded = true
                scmParameters.loading = false
            })
        },
        hasAnyChanged: function(scmParameters) {
            return scmParameters.parameters.filter(function(parameter) {
                return parameter.changed
            }).length > 0
        },
        saveAll: function(scmParameters) {
            var vm = this

            scmParameters.parameters.forEach(function(parameter) {
                if (!parameter.changed)
                    return

                vm.saveParameter(parameter)
            })
        },
        resetAll: function(scmParameters) {
            var vm = this

            scmParameters.parameters.forEach(function(parameter) {
                vm.setDefault(parameter)
                vm.saveParameter(parameter)
            })
        },
        setDefault: function(parameter) {
            parameter.value = parameter.defaultValue
            parameter.changed = true
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
            this.currentScmParameter = null
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
                loaded: false,
                parameters: []
            }

            vm.scmParameters.push(scmParameters)





            vm.scmParameters.sort(compareValues('name'))
        }
    }
})

Vue.component('configure-scms', {
    template: `
<div>
    <div v-if="count == 0">
        Nothing selected yet...
    </div>
    <table class="table is-fullwidth">
        <thead v-if="count > 0">
        <tr class="field">
            <th>Picked up ?</th>
            <th><input class="input" type="text" v-model="filters.name" placeholder="Name"></th>
            <th><input class="input" type="text" v-model="filters.src" placeholder="Source"></th>
            <th><input class="input" type="text" v-model="filters.entry" placeholder="Entry"></th>
            <th>Timeout</th>
            <th>Options</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="scm in filter()">
            <td>
                <span v-if="whoBroughtMe(scm).length > 0"><a @click.stop="showWhoBroughtMe = scm">Yes</a></span>
                <span v-else>No</span>
            </td>
            <td><span>{{scm.name}}</span><br/><span style="color: lightgrey">Last edit: {{getRelativeTimestamp(scm)}}</span></td>
            <td>{{scm.descriptor.src}}</td>
            <td>{{scm.descriptor.entry}}</td>
            <td>{{scm.descriptor.timeout}}</td>
            <td><button class="button is-success" @click.stop="openEdit(scm)">Edit</button></td>
        </tr>
        </tbody>
    </table>

    <div class="modal is-active" v-bind:class=" { 'code-hidden': !editScript } ">
        <div class="modal-background"></div>
        <div class="modal-content code-edit-modal">
            <div class="box" v-click-outside="closeEdit">
                <div class="columns" v-if="editScript">
                    <div class="column is-half">
                        <h1 class="title is-3">Edit</h1>
                        <h4 class="subtitle is-6">file: {{editScript.source}}, source: {{editScript.descriptor.src}}</h4>
                    </div>
                    <div class="column is-half">
                        <div class="buttons has-addons is-right">
                            <button class="button is-success" @click="saveSource()" v-bind:class=" { 'is-loading': sending }">
                                <span class="icon is-small" v-if="saved">
                                    <i class="fas fa-check"></i>
                                </span>
                                <span>Save</span>
                            </button>
                        </div>
                    </div>
                </div>

                <div id="editArea"></div>

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
            count: Object.values(this.value.scms).length,

            codeMirror: null,
            editScript: '',
            showWhoBroughtMe: null,
            sending: false,
            saved: false,
            filters: {
                name: '',
                src: '',
                entry: ''
            }
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
    methods: {
        filter: function() {
            var vm = this

            var filtered = []

            Object.values(vm.value.scms.registry).forEach(function(scm) {
                if (vm.filters.name != '' && scm.name.indexOf(vm.filters.name) < 0) return
                if (vm.filters.src != '' && scm.descriptor.src.indexOf(vm.filters.src) < 0) return
                if (vm.filters.entry != '' && scm.descriptor.entry.indexOf(vm.filters.entry) < 0) return


                filtered.push(scm)
            })

            return filtered.sort(compareValues('name'))
        },
        getRelativeTimestamp: function(scm) {
            var vm = this

            if (vm.value.scms.scripts[scm.source] == undefined)
                return "undefined"

            var lastEdit = vm.value.scms.scripts[scm.source].lastEdit
            return TimeAgo.inWords(lastEdit)
        },
        openEdit: function(scm) {
            var vm = this

            vm.codeMirror.setValue(vm.value.scms.scripts[scm.source].text)
            vm.codeMirror.refresh()
            vm.editScript = scm
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

                vm.sending = false
                vm.saved = true

                var data = response.data
                var registry = vm.value.scms.registry

                // udpate fields
                vm.value.scms.scripts[vm.editScript.source].text = content
                vm.value.scms.scripts[vm.editScript.source].lastEdit = data.scripts[vm.editScript.source].lastEdit

                var tmp = []
                for(var key in registry) {
                    if (!registry.hasOwnProperty(key)) { continue }

                    var desc = vm.value.scms.registry[key]

                    if (desc.source != vm.editScript.source) { continue }

                    tmp.push(key)
                }

                tmp.forEach(function(remove) {
                    delete registry[remove]
                })


                for(var key in data.registry) {
                    if (!data.registry.hasOwnProperty(key)) { continue }

                    registry[key] = data.registry[key]

                    vm.editScript = registry[key]
                }
            })
        },
        whoBroughtMe: function(scm) {
            var vm = this

            if (!scm)
                return

            var whoBroughtMe = []

            vm.value.invs.nodes.forEach(function(chosen) {

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
        },
        closeEdit: function() {
            this.editScript = null

            this.sending = false
            this.saved = false
        },
        closeWhoBroughtMe: function() {
            this.showWhoBroughtMe = ''
        }
    }
})