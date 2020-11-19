xdescribe('loganimation', function() {

    let editorContainer = document.createElement('div');
    editorContainer.id = 'editorcanvas';

    it('should expose globals', function() {
        let LogAnimation = logAnimation.LogAnimation;
        // then
        //expect(LogAnimation).to.exist;
        expect(new LogAnimation()).to.exist;
    });

});