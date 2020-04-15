Vue.component('promote', {
    template: `
<div>
    <p class="title is-5 has-text-centered">Do you wish to promote your latest run?</p>
    <div class="buttons is-centered">
        <button class="button is-danger is-outlined" @click="promote()" :disabled="promoted" v-bind:class=" { 'is-loading': promoting }">
            <span>Sure</span>
            <span class="icon is-small" v-show="promoted">
                <i class="fas fa-check"></i>
            </span>
        </button>
    </div>
    <p class="has-text-centered has-text-danger" v-show="error">An error occurred during promotion. Check logs for details</p>
    <p class="has-text-centered" v-show="promoted">You will be redirected in a short moment....</p>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            promoting: false,
            promoted: false,
            error: false
        }
    },
    methods: {
        promote: function() {
            var vm = this

            vm.promoting = true

            axios.post(vm.value.api.links.review.promote).then(response => {
                vm.promoted = true
                vm.promoting = false

                // Wait 1s to reload
                setTimeout(function() {
                    vm.$bus.$emit('toast', `<strong>Promoted</strong> successfully! Will refresh shortly.`)

                    window.location.href = '#choose'
                    window.location.reload(true)
                }, 1000)
            }).catch(err => {
                vm.promoted = false
                vm.promoting = false
                vm.error = true

                vm.$bus.$emit('toast', `error:Failed to <strong>promote</strong>!`)
            })
        }
    },
    created: function() {
    }
})