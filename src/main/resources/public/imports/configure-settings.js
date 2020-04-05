Vue.component('configure-settings', {
    template: `
<div class="modal" v-bind:class="{ 'is-active': isVisible() }">
    <div class="modal-background"></div>
    <div class="modal-content" style="width: 50%">
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
                var element = document.querySelector('#configure-settings-editarea')
                element.appendChild(elt)
            }, {
                autoRefresh: true,
                lineNumbers: true,
                matchBrackets: true,
                mode: "application/json"
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

            axios.get(vm.value.shared.api.links.settings.default).then(response => {
                vm.codeMirror.setValue(JSON.stringify(response.data, null, 2))
                vm.codeMirror.refresh()

                vm.edited = false
            })
        },
        save: function() {
            var vm = this
            var content = vm.codeMirror.getValue()

            vm.sending = true

            axios.post(vm.value.shared.api.links.settings.save, content, {
                headers: { 'Content-Type': 'text/plain' }
            }).then(response => {

                vm.sending = false
                vm.edited = false
                vm.saved = true
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