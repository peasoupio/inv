Vue.component('configure-run', {
    template: /*html*/ `
<div class="modal is-active code" v-bind:class=" { 'hidden': !isVisible() } ">
    <div class="modal-background"></div>
    <div class="modal-content">
        <div class="box" v-click-outside="close">
            <div class="columns">
                <div class="column">
                    <h1 class="title is-3">Configure run.txt file</h1>
                </div>
                <div class="column is-one-fifth">
                    <div class="buttons has-addons is-right">
                        <button class="button is-success" @click="save()" v-bind:class=" { 'is-loading': sending }" :disabled="!canSave()">
                            <span class="icon is-small" v-if="saved">
                                <i class="fas fa-check"></i>
                            </span>
                            <span>Save</span>
                        </button>
                    </div>
                </div>
            </div>
            <div id="configure-run-editarea"></div>
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            codeMirror: {},
            opened: false,
            edited: false,
            sending: false,
            saved: false,

            accesses: {
                save: false
            }
        }
    },
    methods: {
        isVisible: function() {
            var vm = this

            if (!vm.value.visible)
                return false

            if (!vm.opened) {
                vm.fetch()

                vm.opened = true
            }

            return true
        },
        enableEditor: function() {
            var vm = this

            var codeMirror = CodeMirror(function(elt) {
                var element = document.querySelector('#configure-run-editarea')
                element.appendChild(elt)
            }, {
                autoRefresh: true,
                lineNumbers: true,
                matchBrackets: true
            })

            codeMirror.setSize(null, 640)

            codeMirror.on("change",function(cm,change){
                vm.saved = false
                vm.edited = true
            })

            vm.codeMirror = codeMirror
        },
        fetch: function() {
            var vm = this

            axios.get(vm.value.shared.api.links.runFile.default).then(response => {
                vm.codeMirror.setValue(response.data)
                vm.codeMirror.refresh()

                vm.edited = false
            })
        },
        canSave: function() {
            var vm = this

            if (!vm.accesses.save)
                return false

            if (!vm.edited)
                return false

            return true
        },
        save: function() {
            var vm = this
            var content = vm.codeMirror.getValue()

            vm.sending = true

            axios.post(vm.value.shared.api.links.runFile.save, content, {
                headers: { 'Content-Type': 'text/plain' }
            }).then(response => {

                vm.sending = false
                vm.edited = false
                vm.saved = true

                vm.$bus.$emit('toast', `success:Saved <strong>init file</strong> successfully!`)
            })
            .catch(err => {
                vm.$bus.$emit('toast', `error:Failed to <strong>save init file</strong>!`)

                vm.sending = false
                vm.edited = true
                vm.saved = false
            })
        },
        close: function() {
            var vm = this

            if (vm.edited) {
                var ok = confirm("Clear unsaved work ?")

                if (!ok)
                    return
            }

            // Reload to get latest data
            window.location.reload(true)
        }
    },
    mounted: function() {
        var vm = this

        // Manage accesses
        if (vm.value.shared.api.links.runFile.save !== undefined)
            vm.accesses.save = true

        vm.enableEditor()

    }
})