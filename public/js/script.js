$(function () {

  $body = $("body");

  $(document).on({
    ajaxStart: function() { $body.addClass("loading");    },
    ajaxStop: function() { $body.removeClass("loading"); }
  });

  function request(addr, data, type, isJson) {
    return $.ajax({
      type: type,
      url: addr,
      data: isJson ? JSON.stringify(data) : data,
      contentType: (isJson ? "application/json;" : "plain/text;") + " charset=utf-8",
      dataType: "json"
    });
  }

  function requestJson(addr, data){
    return request(addr, data, "POST", true);
  }

  function callNEL(sentence){
    var addr = "http://localhost:4567/analyze";
    var data = {
        s: sentence
    };
    return requestJson(addr, data);
  }

  function submitSentence(){
    console.log("submitSentence")
    var sentence = $("#sentence").val()
    callNEL(sentence).then( data => renderSentence(sentence, data), () => alert("Something goes wrong, please try other sentence. Do not blame, it is prototype ;)") )
  }

  function renderSentence(sentence, data){
    window.sentence = sentence;
    window.data = data;

    var startPos = 0;
    var newSent = ""

    data.annotations.forEach( x => {
            var beforeText = sentence.substring( startPos, x.fromPos );
            var text = sentence.substring( x.fromPos, x.toPos );
            console.log(x)
            var link = $("<a>").attr('href', x.concepts[0].concept ).text(text).get(0).outerHTML;
            newSent += beforeText + link;
            startPos = x.toPos;
        } )

    newSent += sentence.substr(startPos)

    $("#result").html(newSent);

    console.log(newSent);
  }

  var examples = [
    'The reader should already have some understanding of what is a neural network and in linear algebra before reading this',
    'Religious freedom and free speech is alive and well in America.',
    'I’ve been spending the last few days to understand what is sparse coding, Here are my results!',
    'Isaac Asimov was an American writer and professor of biochemistry at Boston University',
    'Titanic hit iceberg in the Atlantic ocean',
    'James Cameron made Titanic in 1997',
    'Scala source code is intended to be compiled to Java bytecode, so that the resulting executable code runs on a Java virtual machine',
    'In his short story "Evidence" Asimov lets his recurring character Dr. Susan Calvin expound a moral basis behind the Three Laws.'
  ]


  $("#submitButton").on("click", submitSentence)
  $("#exampleButton").on('click', () => $("#sentence").val( examples[Math.floor(Math.random()*examples.length)] ))


});


