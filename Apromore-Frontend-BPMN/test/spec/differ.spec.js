'use strict';

//var TestHelper = require('bpmn-js/../TestHelper');


var _ = require('lodash');

var fs = require('fs');

import BpmnModdle from 'bpmn-moddle';

var Differ = require('../../src/differ'),
    SimpleChangeHandler = require('../../src/change-handler');


//TestHelper.insertCSS('diff.css', require('../../dist/diff.css'));


function importDiagrams(a, b, handleResult) {
  let bpmnModdle = new BpmnModdle();
  let aPromise = bpmnModdle.fromXML(a);
  let bPromise = bpmnModdle.fromXML(b);
  Promise.all([aPromise, bPromise])
      .then(defs => handleResult(null, defs[0].rootElement, defs[1].rootElement))
      .catch(err => handleResult(err));

  /*
  bpmnModdle.fromXML(a, function(err, adefs) {
    if (err) {
      return handleResult(err);
    }

    bpmnModdle.fromXML(b, function(err, bdefs) {
      if (err) {
        return handleResult(err);
      } else {
        return handleResult(null, adefs, bdefs);
      }
    });
  });
   */
}


function diff(a, b, handleResult) {

  importDiagrams(a, b, function(err, adefs, bdefs) {
    if (err) {
      return handleResult(err);
    }

    // given
    var handler = new SimpleChangeHandler();

    //console.log(adefs);

    // when
    new Differ().diff(adefs, bdefs, handler);

    handleResult(err, handler, adefs, bdefs);
  });

}


describe('diffing', function() {

  describe('diff', function() {

    it('should discover add', function(done) {

      // var aDiagram = require('../fixtures/add/before.bpmn');
      // var bDiagram = require('../fixtures/add/after.bpmn');

      var aDiagram = require('../fixtures/add/before.bpmn').default;
      var bDiagram = require('../fixtures/add/after.bpmn').default;

      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }


        // then
        // expect(results._added).to.have.keys([ 'EndEvent_1', 'SequenceFlow_2' ]);
        // expect(results._removed).to.eql({});
        // expect(results._layoutChanged).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._added)).toEqual(jasmine.arrayContaining([ 'EndEvent_1', 'SequenceFlow_2' ]));
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._layoutChanged)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });

    });


    it('should discover remove', function(done) {

      // var aDiagram = require('../fixtures/remove/before.bpmn');
      // var bDiagram = require('../fixtures/remove/after.bpmn');

      var aDiagram = require('../fixtures/remove/before.bpmn').default;
      var bDiagram = require('../fixtures/remove/after.bpmn').default;

      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._removed).to.have.keys([ 'Task_1', 'SequenceFlow_1' ]);
        // expect(results._added).to.eql({});
        // expect(results._layoutChanged).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._removed)).toEqual(jasmine.arrayContaining([ 'Task_1', 'SequenceFlow_1' ]));
        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._layoutChanged)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });

    });


    it('should discover change', function(done) {

      // var aDiagram = require('../fixtures/change/before.bpmn');
      // var bDiagram = require('../fixtures/change/after.bpmn');

      var aDiagram = require('../fixtures/change/before.bpmn').default;
      var bDiagram = require('../fixtures/change/after.bpmn').default;

      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._added).to.eql({});
        // expect(results._removed).to.eql({});
        // expect(results._layoutChanged).to.eql({});
        // expect(results._changed).to.have.keys([ 'Task_1'  ]);

        // expect(results._changed['Task_1'].attrs).to.deep.eql({
        //   name: { oldValue: undefined, newValue: 'TASK'}
        // });

        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._layoutChanged)).toEqual({});
        expect(Object.keys(results._changed)).toEqual(jasmine.arrayContaining([ 'Task_1']));

        expect(Object.assign({}, results._changed['Task_1'].attrs)).toEqual({
          name: { oldValue: undefined, newValue: 'TASK'}
        });

        done();
      });

    });


    it('should discover layout-change', function(done) {

      // var aDiagram = require('../fixtures/layout-change/before.bpmn');
      // var bDiagram = require('../fixtures/layout-change/after.bpmn');

      var aDiagram = require('../fixtures/layout-change/before.bpmn').default;
      var bDiagram = require('../fixtures/layout-change/after.bpmn').default;

      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._layoutChanged).to.have.keys([ 'Task_1', 'SequenceFlow_1' ]);
        // expect(results._added).to.eql({});
        // expect(results._removed).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining([ 'Task_1', 'SequenceFlow_1']));
        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });

    });

  });


  describe('api', function() {

    it('should diff with default handler', function(done) {

      // var aDiagram = require('../fixtures/layout-change/before.bpmn');
      // var bDiagram = require('../fixtures/layout-change/after.bpmn');

      var aDiagram = require('../fixtures/layout-change/before.bpmn').default;
      var bDiagram = require('../fixtures/layout-change/after.bpmn').default;

      // when
      importDiagrams(aDiagram, bDiagram, function(err, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // when
        var results = new Differ().diff(aDefinitions, bDefinitions);

        // then
        // expect(results._layoutChanged).to.have.keys([ 'Task_1', 'SequenceFlow_1' ]);
        // expect(results._added).to.eql({});
        // expect(results._removed).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining([ 'Task_1', 'SequenceFlow_1']));
        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });

    });


    it('should diff via static diff', function(done) {

      var aDiagram = require('../fixtures/layout-change/before.bpmn').default;
      var bDiagram = require('../fixtures/layout-change/after.bpmn').default;

      // when
      importDiagrams(aDiagram, bDiagram, function(err, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // when
        var results = Differ.diff(aDefinitions, bDefinitions);

        // then
        // expect(results._layoutChanged).to.have.keys([ 'Task_1', 'SequenceFlow_1' ]);
        // expect(results._added).to.eql({});
        // expect(results._removed).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining([ 'Task_1', 'SequenceFlow_1']));
        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });

    });

  });


  describe('scenarios', function() {


    it('should diff collaboration pools / lanes', function(done) {

      var aDiagram = require('../fixtures/collaboration/before.bpmn').default;
      var bDiagram = require('../fixtures/collaboration/after.bpmn').default;


      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._added).to.have.keys([ 'Participant_2' ]);
        // expect(results._removed).to.have.keys([ 'Participant_1', 'Lane_1' ]);
        // expect(results._layoutChanged).to.have.keys([ '_Participant_2', 'Lane_2' ]);
        // expect(results._changed).to.have.keys([ 'Lane_2' ]);
        expect(Object.keys(results._added)).toEqual(jasmine.arrayContaining([ 'Participant_2']));
        expect(Object.keys(results._removed)).toEqual(jasmine.arrayContaining([ 'Participant_1', 'Lane_1']));
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining([ '_Participant_2', 'Lane_2']));
        expect(Object.keys(results._changed)).toEqual(jasmine.arrayContaining([ 'Lane_2']));

        done();
      });
    });


    it('should diff pizza collaboration StartEvent move', function(done) {

      var aDiagram = require('../fixtures/pizza-collaboration/start-event-old.bpmn').default;
      var bDiagram = require('../fixtures/pizza-collaboration/start-event-new.bpmn').default;


      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._layoutChanged).to.have.keys([ '_6-61' ]);
        // expect(results._added).to.eql({});
        // expect(results._removed).to.eql({});
        // expect(results._changed).to.eql({});
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining([ '_6-61']));
        expect(Object.assign({}, results._added)).toEqual({});
        expect(Object.assign({}, results._removed)).toEqual({});
        expect(Object.assign({}, results._changed)).toEqual({});

        done();
      });
    });


    it('should diff pizza collaboration', function(done) {

      var aDiagram = require('../fixtures/pizza-collaboration/old.bpmn').default;
      var bDiagram = require('../fixtures/pizza-collaboration/new.bpmn').default;


      // when
      diff(aDiagram, bDiagram, function(err, results, aDefinitions, bDefinitions) {

        if (err) {
          return done(err);
        }

        // then
        // expect(results._added).to.have.keys([
        //   'ManualTask_1',
        //   'ExclusiveGateway_1'
        // ]);
        expect(Object.keys(results._added)).toEqual(jasmine.arrayContaining([ 'ManualTask_1', 'ExclusiveGateway_1']));

        // expect(results._removed).to.have.keys([
        //   '_6-674', '_6-691', '_6-746', '_6-748', '_6-74', '_6-125', '_6-178', '_6-642'
        // ]);
        expect(Object.keys(results._removed)).toEqual(
            jasmine.arrayContaining(['_6-674', '_6-691', '_6-746', '_6-748', '_6-74', '_6-125', '_6-178', '_6-642']));

        // expect(results._layoutChanged).to.have.keys([
        //   '_6-61'
        // ]);
        expect(Object.keys(results._layoutChanged)).toEqual(jasmine.arrayContaining(['_6-61']));

        // expect(results._changed).to.have.keys([ '_6-127' ]);
        expect(Object.keys(results._changed)).toEqual(jasmine.arrayContaining(['_6-127']));

        done();
      });
    });

  });
});