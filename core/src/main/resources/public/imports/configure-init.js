Vue.component('configure-init', {
    template: `
<div class="modal is-active code" v-bind:class=" { 'hidden': !isVisible() } ">
    <div class="modal-background"></div>
    <div class="modal-content">
        <div class="box" v-click-outside="close">
            <div class="columns">
                <div class="column">
                    <h1 class="title is-3">Configure init file</h1>
                </div>
                <div class="column is-one-fifth">
                    <div class="buttons has-addons is-right">
                        <button class="button is-success" @click="save()" v-bind:class=" { 'is-loading': sending }" :disabled="!edited">
                            <span class="icon is-small" v-if="saved">
                                <i class="fas fa-check"></i>
                            </span>
                            <span>Save</span>
                        </button>
                    </div>
                </div>
            </div>
            <div id="configure-init-editarea"></div>

            <div v-if="errorCount > 0">
                <p>Compilation error(s) caught:</p>
                <p class="has-text-danger" v-for="error in errors">{{error}}</p>
            </div>
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
            errorCount: 0,
            errors: []
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
        openEditor: function() {
            var vm = this

            var codeMirror = CodeMirror(function(elt) {
                var element = document.querySelector('#configure-init-editarea')
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
        },
        fetch: function() {
            var vm = this

            axios.get(vm.value.shared.api.links.initFile.default).then(response => {
                vm.codeMirror.setValue(response.data)
                vm.codeMirror.refresh()

                vm.edited = false
            })
        },
        save: function() {
            var vm = this
            var content = vm.codeMirror.getValue()

            vm.sending = true

            axios.post(vm.value.shared.api.links.initFile.save, content, {
                headers: { 'Content-Type': 'text/plain' }
            }).then(response => {
                vm.errorCount = response.data.errorCount
                vm.errors = response.data.errors

                vm.sending = false
                vm.edited = false

                if (vm.errorCount == 0) {
                    vm.saved = true

                    vm.$bus.$emit('toast', `success:Saved <strong>init file</strong> successfully!`)
                }
            })
            .catch(err => {
                vm.$bus.$emit('toast', `error:Failed to <strong>save init file</strong>!`)
            })
        },
        close: function() {
            var vm = this

            if (vm.edited) {
                var ok = confirm("Clear unsaved work ?")

                if (!ok)
                    return
            }

            vm.opened = false
            vm.saved = false
            vm.value.visible = false
        }
    },
    mounted: function() {
        this.openEditor()
    }
})