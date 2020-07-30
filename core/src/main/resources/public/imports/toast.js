Vue.component('toast', {
    template: `
<div class="toast fadeout" v-if="message" :key="updateIndex">
    <div class="notification" v-bind:class="styleClass">
        <p class="has-text-centered" v-html="message"></p>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            message: '',
            styleClass: 'is-link',
            latestToast: null,
            updateIndex: 0
        }
    },
    methods: {
        showToast: function(message) {
            var vm = this

            // Check if already showing something
            if (!message)
                return

            // Remove existing toast timeout
            if (vm.latestToast)
                clearInterval(vm.latestToast)

            // Get which styleClass to use
            if (message.startsWith('error:')) {
                vm.styleClass = 'is-danger'
                vm.message = message.substring(6)
            } else if (message.startsWith('warn:')) {
                vm.styleClass = 'is-primary'
                vm.message = message.substring(5)
            } else if (message.startsWith('success:')) {
                vm.styleClass = 'is-success'
                vm.message = message.substring(8)
            } else {
                vm.styleClass = 'is-link'
                vm.message = message
            }

            // Set timeout
            vm.latestToast = setTimeout(function() {
                vm.message = ''
                vm.latestToast =null

                vm.$forceUpdate()
            }, 4000)

            // Force re-update of element
            vm.updateIndex++
        }
    },
    created: function() {
        this.$bus.$on('toast', this.showToast)
    }
})