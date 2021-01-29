Vue.component('configure-settings', {
    template: `
<div class="modal is-active code" v-bind:class=" { 'hidden': !isVisible() } ">
    <div class="modal-background"></div>
    <div class="modal-content">
        <div class="box" v-click-outside="close">
            <div class="columns">
                <div class="column">
                    <h1 class="title is-3">Configure settings.xml file</h1>
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
            <div id="configure-settings-editarea"></div>
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
            saved: false
        }
    },
    methods: {

        isVisible: function() {
            const vm = this

            if (!vm.value.visible)
                return false

            if (!vm.opened) {
                vm.fetch()

                vm.opened = true
            }

            return true
        },

        openEditor: function() {
            const vm = this

            const codeMirror = CodeMirror(function(elt) {
                const element = document.querySelector('#configure-settings-editarea')
                element.appendChild(elt)
            }, {
                autoRefresh: true,
                lineNumbers: true,
                matchBrackets: true,
                mode: "application/ld+json"
            })

            codeMirror.setSize(null, 640)

            codeMirror.on("change",function(){
                vm.saved = false
                vm.edited = true
            })

            vm.codeMirror = codeMirror
        },

        fetch: function() {
            const vm = this

            axios.get(vm.value.shared.api.links.settings.default).then(response => {
                vm.codeMirror.setValue(JSON.stringify(response.data, null, 2))
                vm.codeMirror.refresh()

                vm.edited = false
            })
        },

        save: function() {
            const vm = this
            const content = vm.codeMirror.getValue()

            vm.sending = true

            axios.post(vm.value.shared.api.links.settings.save, content, {
                headers: { 'Content-Type': 'text/plain' }
            }).then(() => {

                vm.sending = false
                vm.edited = false
                vm.saved = true

                vm.$bus.$emit('toast', `success:Saved <strong>settings.xml</strong> successfully!`)
            })
            .catch(() => {
                vm.$bus.$emit('toast', `error:Failed <strong>to save settings.xml</strong>!`)
            })
        },
        close: function() {
            const vm = this

            if (vm.edited) {
                const ok = confirm("Clear unsaved work ?")

                if (!ok)
                    return
            }

            // Reload to get latest data
            window.location.reload(true)
        }
    },
    mounted: function() {
        this.openEditor()
    }
})