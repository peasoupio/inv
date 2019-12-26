Vue.component('pagination', {
    template: `
<nav class="pagination is-centered" role="navigation" aria-label="pagination" v-if="value">
    <button class="pagination-previous" title="This is the first page" :disabled="value.from <= 0" @click="seePrevious()">Previous</button>
    <button class="pagination-next" :disabled="value.from + value.step > value.total" @click="seeNext()">Next page</button>

    <ul class="pagination-list">
        <li v-if="isStartOutOfSight()"><a class="pagination-link" @click="seeAt(0)">1</a></li>
        <li v-if="isStartOutOfSight()"><span class="pagination-ellipsis">&hellip;</span></li>

        <li v-for="index in indexes()">
            <a class="pagination-link " aria-current="page" @click="seeAt(index)" v-bind:class="{ 'is-current' : index == currentIndex }">{{index + 1}}</a>
        </li>

        <li v-if="isEndOutOfSight()"><span class="pagination-ellipsis">&hellip;</span></li>
        <li v-if="isEndOutOfSight()"><a class="pagination-link" @click="seeAt(remainings - 1)">{{remainings}}</a></li>
    </ul>
</nav>
`,
    props: ['value'],
    data: function() {
        return {
            threshold: 4,
            currentIndex:  0
        }
    },
    computed: {
        remainings: {
            get() {
                return Math.ceil(this.value.total / this.value.step)
            }
        },
        startSlice: {
            get() {
                var startSlice = 0
                if (this.currentIndex - this.threshold > 0)
                    startSlice = this.currentIndex - this.threshold

                return startSlice
            }
        },
        endSlice: {
            get() {
                var endSlice = this.remainings

                if (this.currentIndex + this.threshold < this.remainings)
                    endSlice = Math.max(this.threshold * 2, this.currentIndex + this.threshold)

                return endSlice
            }
        }
    },
    methods: {
        seeNext: function() {
            var vm = this

            ++vm.currentIndex
            vm.value.from += vm.value.step
            vm.value.refresh(vm.value.from)
        },
        seePrevious: function() {
            var vm = this

            --vm.currentIndex
            vm.value.from -= vm.step
            vm.value.refresh(vm.value.from)
        },
        indexes: function() {
            var vm = this

            var indexes = []

            if (vm.value.from == 0)
                vm.currentIndex = 0

            for(var i = 0; i < this.remainings; i++) {
                indexes.push(i)
            }

            return indexes.slice(this.startSlice, this.endSlice)
        },
        seeAt: function(index) {
            var vm = this

            vm.currentIndex = index
            vm.value.from = index * vm.value.step

            vm.value.refresh(vm.value.from)
        },
        isStartOutOfSight: function() {
            return (this.currentIndex - this.threshold) > 1
        },
        isEndOutOfSight: function() {
            return (this.currentIndex + this.threshold) < this.remainings
        }
    }
})
