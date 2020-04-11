Vue.component('choose', {
    template: `
<div>
    <div class="columns">
        <div class="column is-2" style="width: 20%">
            <tab-tiles v-model="tabTilesSettings" v-if="value.setup.firstTime != undefined" />
        </div>

        <div class="column" v-show="currentTab.template">
            <component v-bind:is="currentTab.template" v-model="value" />
        </div>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            currentTab: {}
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'INV', description: 'Select INVs by their name', template: 'choose-inv', disabled: vm.value.setup.firstTime },
                        { label: 'Broadcast', description: 'Select INVs by their specific broadcast(s)', template: 'choose-broadcast', disabled: vm.value.setup.firstTime },
                        { label: 'SCM', description: 'Select INVs by their specific SCM', template: 'choose-scm'}
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()
                    }
                }
            }
        }
    }
})

