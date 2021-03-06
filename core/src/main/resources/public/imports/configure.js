Vue.component('configure', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2">
            <tab-tiles v-model="tabTilesSettings" />
        </div>

        <div class="column" v-if="currentTab.template">
            <component v-bind:is="currentTab.template" v-model="value" />
        </div>
    </div>

</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentTab: 'parameters'
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'Params', description: 'Configure REPO parameters for each SELECTED INVs', template: 'configure-parameters'}
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()
                    }
                }
            }
        }
    },
    methods: {
        completedCount: function() {
            var vm = this

            if (vm.value.selectedRepos.descriptors.length == 0)
                return 0

            var count = 0

            vm.value.selectedRepos.descriptors.forEach(function(repo) {
                if (!repo.completed) return

                count++
            })

            return count
        }
    }
})