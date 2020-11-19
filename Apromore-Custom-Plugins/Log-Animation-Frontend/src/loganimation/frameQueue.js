'use strict';

export default class FrameQueue {
    constructor() {
        this.itemMap = new Map();
        this.insertionIndex = 0;
        this.removalIndex = 0;
    }

    queue(element) {
        this.itemMap.set(this.insertionIndex, element);
        this.insertionIndex++;
    }

    queueMany(elements) {
        if (!(elements instanceof Array)) return;
        for (let element of elements) {
            this.queue(element);
        }
    }

    dequeue() {
        const el = this.itemMap.get(this.removalIndex);
        if (typeof el !== 'undefined') {
            this.itemMap.delete(this.removalIndex);
            this.removalIndex++;
        }
        return el;
    }

    dequeueMany(count) {
        for (let i=0;i<count;i++) {
            if (this.dequeue() === undefined) return;
        }
    }

    size() {
        return this.itemMap.size;
    }

    clear() {
        this.itemMap.clear();
        this.insertionIndex = 0;
        this.removalIndex = 0;
    }
}
