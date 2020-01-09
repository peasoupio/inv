Vue.component('tab-tiles', {
    template: `
<div>
    <div class="tile"
         v-for="tab in value.tabs"
         @click="setTab(tab)">
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
