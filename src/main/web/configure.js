Vue.component('configure', {
    template: `
<div>

    <p class="title is-1">Configure: </p>

    <div class="tabs">
        <ul>
            <li v-bind:class="{ 'is-active' : activeTab=='scms' }"><a v-on:click="activeTab='registry'">Scms ({{Object.values(value.scms.registry).length}})</a></li>
        </ul>
    </div>

    <configure-scms v-model="value" v-if="activeTab=='scms'"></configure-scms>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: 'scms'
        }
    },
    methods: {

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
                <span v-if="whoBroughtMe(scm).length > 0"><a v-on:click="showWhoBroughtMe = scm">Yes</a></span>
                <span v-else>No</span>
            </td>
            <td><span>{{scm.name}}</span><br/><span style="color: lightgrey">Last edit: {{getRelativeTimestamp(scm)}}</span></td>
            <td>{{scm.descriptor.src}}</td>
            <td>{{scm.descriptor.entry}}</td>
            <td>{{scm.descriptor.timeout}}</td>
            <td><button class="button is-success" v-on:click="openEdit(scm)">Edit</button></td>
        </tr>
        </tbody>
    </table>

    <div class="modal is-active" v-bind:class=" { 'code-hidden': !editScript } ">
        <div class="modal-background"></div>
        <div class="modal-content code-edit-modal">
            <div class="box">
                <div class="columns" v-if="editScript">
                    <div class="column is-half">
                        <h1 class="title is-3">Edit</h1>
                        <h4 class="subtitle is-6">file: {{editScript.source}}, source: {{editScript.descriptor.src}}</h4>
                    </div>
                    <div class="column is-half">
                        <div class="buttons has-addons is-right">
                            <button class="button is-success" v-on:click="saveSource()" v-bind:class=" { 'is-loading': sending }">
                                <span class="icon is-small">
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
        <button class="modal-close is-large" aria-label="close" v-on:click="editScript = null"></button>
    </div>

    <div class="modal is-active" v-if="showWhoBroughtMe">
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
                    <tr v-for="other in whoBroughtMe(showWhoBroughtMe)">
                        <td>{{other.owner}}</td>
                        <td>{{other.name}}</td>
                        <td>{{other.id}}</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
        <button class="modal-close is-large" aria-label="close" v-on:click="showWhoBroughtMe = null"></button>
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
    }
})