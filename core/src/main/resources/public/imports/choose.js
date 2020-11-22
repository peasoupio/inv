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
            defaultTab: {},
            currentTab: {}
        }
    },
    computed: {
        tabTilesSettings: {
            get() {
                var vm = this

                return {
                    tabs: [
                        { label: 'Tags', description: 'Select INVs by its tags', template: 'choose-tags', disabled: vm.value.setup.firstTime },
                        { label: 'INV', description: 'Select INVs by its name', template: 'choose-inv', disabled: vm.value.setup.firstTime },
                        { label: 'Broadcast', description: 'Select INVs by its specific broadcasts', template: 'choose-broadcast', disabled: vm.value.setup.firstTime },
                        { label: 'REPO', description: 'Select INVs by its specific REPO', template: 'choose-repo'}
                    ],
                    tabSet: function(tab) {
                        vm.currentTab = tab
                        vm.$forceUpdate()

                        localStorage.chooseCurrentTab = tab.template
                    },
                    defaultTab: vm.defaultTab
                }
            }
        }
    },
    created: function() {
        if (!localStorage.chooseCurrentTab)
            return

        var vm = this
        vm.tabTilesSettings.tabs.forEach(tab => {
            if (tab.template != localStorage.chooseCurrentTab)
                return

            vm.defaultTab = tab
        })
    }
})

