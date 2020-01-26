Vue.component('promote', {
    template: `
<div>
    <p class="title is-5 has-text-centered">Do you wish to promote your latest run?</p>
    <div class="buttons is-centered">
        <button class="button is-danger" @click="promote()" :disabled="promoted">
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
            promoted: false,
            error: false
        }
    },
    methods: {
        promote: function() {
            var vm = this

            axios.post(vm.value.api.links.review.promote).then(response => {
                vm.promoted = true
                window.location.href = '#choose'
                window.location.reload(true)
            }).catch(reponse => {
                vm.promoted = false
                vm.error = true
            })
        }
    },
    created: function() {
    }
})