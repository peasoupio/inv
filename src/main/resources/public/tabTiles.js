Vue.component('tab-tiles', {
    template: `
<div style="position: sticky !important; top: 1em">
    <div class="tile"
         v-for="tab in value.tabs"
         @click="clickTab(tab)">
        <article class="tile is-child notification" :class="'is-' + tab.isWhat">
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
        clickTab: function(tab) {
            var vm = this
            var element = window.document.scrollingElement

            element.scrollTo(0, vm.$parent.$el.offsetTop)

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
        this.setTab(this.value.tabs[0])
    }
})
