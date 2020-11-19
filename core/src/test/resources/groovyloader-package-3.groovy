package something.that.will.be.replaced

class AClass {
    @Override
    String toString() {
        return "ACLASS: ${this.getClass().canonicalName}"
    }
}