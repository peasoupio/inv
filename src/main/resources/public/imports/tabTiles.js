Vue.component('tab-tiles', {
    template: `
<div style="position: sticky !important; top: 6em">
    <div class="tile"
         v-for="tab in availableTabs()"
         @click="clickTab(tab)"
         style="margin-bottom: 1em;">
        <article class="tile is-child notification" :class="tab.isWhat !== undefined ? 'is-' + tab.isWhat : 'is-primary'">
            <div class="content">
                <p class="title">
                    {{tab.label}}
                    <span v-show="activeTab==tab"><i class="fas fa-check-square"></i></span>
                    <span v-show="activeTab!=tab"><i class="far fa-square"></i></span>
                </p>
                <p class="subtitle">{{tab.description}}</p>
            </div>
        </article>
    </div>
</div>
`,
    props: ['value'],
    data: function() {
        return {
            activeTab: {}
        }
    },
    computed: {

    },
    methods: {
        availableTabs: function() {
            var vm = this

            return vm.value.tabs.filter(function(tab) {
                return !tab.disabled
            })
        },
        clickTab: function(tab) {
            var vm = this

            // Is it already active?
            if (vm.activeTab == tab)
                return

            var element = window.document.scrollingElement

            element.scrollTo(0, 0)

            var scrollIndex = 0
            var lastScrollIndex = -1
            window.onscroll = function() {
                scrollIndex++
            }

            var interval = setInterval(function() {

                if (lastScrollIndex != scrollIndex) {
                    lastScrollIndex = scrollIndex
                    return
                }

                vm.setTab(tab)

                clearInterval(interval)

                window.onscroll = null
            }, 50);
        },
        setTab: function(tab) {
            var vm = this

            vm.activeTab=tab
            vm.value.tabSet(tab)
        }
    },
    mounted: function() {
        var tabs = this.availableTabs()
        if (tabs.length == 0)
            return

        this.setTab(tabs[0])
    }
})
