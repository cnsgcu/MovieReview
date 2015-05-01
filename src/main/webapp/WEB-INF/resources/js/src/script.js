(function() {
    var sse;

    var Movie = React.createClass({
        render: function () {
            return (
                <div className="movie" onClick={this.props.clickHandler}>
                    <h3>{this.props.title} - {this.props.year}</h3>

                    {this.props.children}
                </div>
            );
        }
    });

    var Review = React.createClass({
        buildHeader: function(score) {
            switch (score) {
                case 0:
                    return (
                        <h4>
                            <span className="glyphicon glyphicon-thumbs-down"></span>
                            <span className="glyphicon glyphicon-thumbs-down"></span>
                        </h4>
                    );
                case 1:
                    return (
                        <h4>
                            <span className="glyphicon glyphicon-thumbs-down"></span>
                        </h4>
                    );
                case 2:
                    return (
                        <h4>
                            <span className="glyphicon glyphicon-thumbs-up"></span>
                            <span className="glyphicon glyphicon-thumbs-down"></span>
                        </h4>
                    );
                case 3:
                    return (
                        <h4>
                            <span className="glyphicon glyphicon-thumbs-up"></span>
                        </h4>
                    );
                case 4:
                    return (
                        <h4>
                            <span className="glyphicon glyphicon-thumbs-up"></span>
                            <span className="glyphicon glyphicon-thumbs-up"></span>
                        </h4>
                    );
                default:
                    return (<h4>Unexpected score: {score}</h4>);
            }
        },

        render: function () {
            var self = this;

            var reviews = this.props.reviews.map(function(review) {
                var sentiments = review["sentiments"].map(function(sentiment) {
                    var opinions = sentiment["opinions"].map(function(opinion) {
                        var bgs = [
                            "bs-callout-danger",
                            "bs-callout-warning",
                            "bs-callout-default",
                            "bs-callout-success",
                            "bs-callout-primary"
                        ];

                        return (
                            <div className={"bs-callout " + bgs[opinion["score"]]}>
                                {self.buildHeader(opinion["score"])}
                                {opinion["sentence"]}
                            </div>
                        );
                    });

                    return (
                        <div>
                            <h4>{sentiment["topic"]}</h4>
                            {opinions}
                        </div>
                    );
                });

                return (
                    <div className="review">
                        <div>{review["review"]}</div>
                        {sentiments}
                    </div>
                );
            });

            return (
                <div className="review_list">
                    {reviews}
                </div>
            );
        }
    });

    var MovieList = React.createClass({
        getReviews: function(movieId) {
            var self = this;

            self.setState({reviews: []});

            if (sse) {
                sse.close();
            }

            sse = new EventSource("/review/" + movieId);

            sse.addEventListener("message", function(msg) {
                var data = JSON.parse(msg.data);

                self.setState({reviews: self.state.reviews.concat([data])});
            }, false);

            sse.addEventListener("eos", function (msg) {
                console.log("Close stream!");
                sse.close();
            }, false);
        },

        getInitialState: function() {
            return {reviews: []};
        },

        render: function () {
            var self = this,
                movies = this.props.data.map(function(movie) {
                    var synopsis = "No synopsis.",
                        boundClick = self.getReviews.bind(self, movie.mId);

                    if (movie.mSynopsis) {
                        synopsis = movie.mSynopsis;
                    }

                    return (
                        <Movie clickHandler={boundClick} title={movie.mTitle} year={movie.mYear}>
                            {synopsis}
                        </Movie>
                    );
                });

            return (
                <div className="result_container">
                    <div className="movie_list">
                        {movies}
                    </div>

                    <Review reviews={this.state.reviews}/>
                </div>
            );
        }
    });

    var MovieReview = React.createClass({
        getInitialState: function() {
            return {movies: []};
        },

        handleKeyUp: function() {
            var self = this,
                movieName = this.refs.search.getDOMNode().value;

            if (movieName) {
                self.setState({movies: []});

                $.ajax({
                    type: "GET",
                    url: "movie",
                    contentType: "text/html; charset=utf-8",
                    data: {name: movieName},
                    success: function(resp) {
                        var rst = resp;

                        if (rst["query"] === self.refs.search.getDOMNode().value) {
                            self.setState({movies: rst["movies"].sort(function(lhs, rhs) {return rhs.mYear - lhs.mYear;})});
                        }
                    }
                });
            } else {
                self.setState({movies: []});
            }
        },

        render: function() {
            return (
                <div>
                    <div id="container">
                        <div className="logo text-primary">
                            <img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAhElEQVRoge3SwQmAMBQE0RRlsfZmMdpBiCI6O+yHuYqP7Bg92G37cRpyQuivNfu/QtCQ5JwQzbQKoUGSc0I00yqEBknOCdFMqxAaJDknZPp0P92jad35yFf3OoTQMiQ5J0QzrUJokOScEM20CqFBknNCNNMqhAZJzgnRTEsDSc4D6cHuAicWBtzYwlivAAAAAElFTkSuQmCC" width="50" height="50" />
                            Movie Review Analysis
                        </div>

                        <div className="col-lg-5 input-group">
                            <input ref="search"
                                   onKeyUp={this.handleKeyUp}
                                   className="form-control"
                                   placeholder="Search movie"
                                   type="text" />

                        <span className="input-group-btn">
                            <button className="btn btn-info">
                                <span className="glyphicon glyphicon-search"></span>
                            </button>
                        </span>
                        </div>
                    </div>


                    <MovieList data={this.state.movies} />
                </div>
            );
        }
    });

    $(document).ready(function() {
        React.render(
            <MovieReview />,
            document.getElementById("app")
        );
    });
})();