describe('test with jasmine-jquery', function () {
    it('should load many fixtures into DOM', function () {
        jasmine.getFixtures().fixturesPath = 'spec/javascripts/fixtures';
        loadFixtures('fixture_LogAnimation.html');
        expect($('#jasmine-fixtures')).toSomething();
    });

    // it('should only return fixture', function () {
    //     let fixture = readFixtures('my_fixture_3.html');
    //     expect(fixture).toSomethingElse();
    // });
});