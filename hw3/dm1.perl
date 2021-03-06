#!/usr/bin/perl

#
# Perl subroutine that generates Indri dependence model queries.
#
# Written by: Don Metzler (metzler@cs.umass.edu)
# Last update: 06/27/2005
#
# Feel free to distribute, edit, modify, or mangle this code as you see fit. If you make any interesting
# changes please email me a copy.
#
# For more technical details, see:
#
#    * Metzler, D. and Croft, W.B., "A Markov Random Field Model for Term Dependencies," ACM SIGIR 2005.
#
#    * Metzler, D., Strohman T., Turtle H., and Croft, W.B., "Indri at TREC 2004: Terabyte Track", TREC 2004.
#
#    * http://ciir.cs.umass.edu/~metzler/
#
# MODIFICATIONS
#  - Updated by Jamie Callan:  02/11/2015
#    Modified to support a less cryptic Indri-like query language.
#    #combine --> #and, #1 --> #near/1, #weight --> #wand, and #uw --> #window/
#
# NOTES
#
#    * this script assumes that the query string has already been parsed and that all characters
#      that are not compatible with Indri's query language have been removed.
#
#    * it is not advisable to do a 'full dependence' variant on long strings because of the exponential
#      number of terms that will result. it is suggested that the 'sequential dependence' variant be
#      used for long strings. either that, or split up long strings into smaller cohesive chunks and
#      apply the 'full dependence' variant to each of the chunks.
#
#    * the unordered features use a window size of 4 * number of terms within the phrase. this has been
#      found to work well across a wide range of collections and topics. however, this may need to be
#      modified on an individual basis.
#

# example usage
#print formulate_query( "white house rose garden", "sd", 0.5, 0.25, 0.25 ) . "\n";
print "1: " . formulate_query( "obama family tree", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "4: " . formulate_query( "toilet", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "5: " . formulate_query( "mitchell college", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "6: " . formulate_query( "kcs", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "7: " . formulate_query( "air travel information", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "8: " . formulate_query( "appraisals", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "9: " . formulate_query( "used car parts", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "11: " . formulate_query( "gmat prep classes", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "15: " . formulate_query( "espn sports", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "16: " . formulate_query( "arizona game and fish", "sd",  0.5, 0.5, 0.5) . "\n";
print "17: " . formulate_query( "poker tournaments", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "18: " . formulate_query( "wedding budget calculator", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "21: " . formulate_query( "volvo", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "22: " . formulate_query( "rick warren", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "28: " . formulate_query( "inuyasha", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "29: " . formulate_query( "ps 2 games", "sd",  0.5, 0.5, 0.5 ) . "\n";
print "30: " . formulate_query( "diabetes education", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "37: " . formulate_query( "pampered chef", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "38: " . formulate_query( "dogs for adoption", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "40: " . formulate_query( "michworks", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "42: " . formulate_query( "the music man", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "45: " . formulate_query( "solar panels", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "47: " . formulate_query( "indexed annuity", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "48: " . formulate_query( "wilson antenna", "sd", 0.5, 0.5, 0.5 ) . "\n";
print "49: " . formulate_query( "flame designs", "sd", 0.5, 0.5, 0.5 ) . "\n";
#print formulate_query( "white house rose garden", "fd", 0.8, 0.1, 0.1 ) . "\n\n";

#
# formulates a query based on query text and feature weights
#
# arguments:
#    * query - string containing original query terms separated by spaces
#    * type  - string. "sd" for sequential dependence or "fd" for full dependence variant. defaults to "fd".
#    * wt[0] - weight assigned to term features
#    * wt[1] - weight assigned to ordered (#near) features
#    * wt[2] - weight assigned to unordered (#window) features
#
sub formulate_query {
    my ( $q, $type, @wt ) = @_;

    # trim whitespace from beginning and end of query string
    $q =~ s/^\s+|\s+$//g;

    my $queryT = "#and( ";
    my $queryO = "#and(";
    my $queryU = "#and(";

    # generate term features (f_T)
    my @terms = split(/\s+/ , $q);
    my $term;
    foreach $term ( @terms ) {
	$queryT .= "$term ";
    }

    my $num_terms = @terms;

    # skip the rest of the processing if we're just
    # interested in term features or if we only have 1 term
    if( ( $wt[1] == 0.0 && $wt[2] == 0.0 ) || $num_terms == 1 ) {
	return $queryT . ")";
    }

    # generate the rest of the features
    my $start = 1;
    if( $type eq "sd" ) { $start = 3; }
    for( my $i = $start ; $i < 2 ** $num_terms ; $i++ ) {
	my $bin = unpack("B*", pack("N", $i)); # create binary representation of i
	my $num_extracted = 0;
	my $extracted_terms = "";

	# get query terms corresponding to 'on' bits
	for( my $j = 0 ; $j < $num_terms ; $j++ ) {
	    my $bit = substr($bin, $j - $num_terms, 1);
	    if( $bit eq "1" ) {
		$extracted_terms .= "$terms[$j] ";
		$num_extracted++;
	    }
	}

	if( $num_extracted == 1 ) { next; } # skip these, since we already took care of the term features...
	if( $bin =~ /^0+11+[^1]*$/ ) { # words in contiguous phrase, ordered features (f_O)
	    $queryO .= " #near/1( $extracted_terms) ";
	}
	$queryU .= " #window/" . 4*$num_extracted . "( $extracted_terms) "; # every subset of terms, unordered features (f_U)
	if( $type eq "sd" ) { $i *= 2; $i--; }
    }

    my $query = "#wand(";
    if( $wt[0] != 0.0 && $queryT ne "#and( " ) { $query .= " $wt[0] $queryT)"; }
    if( $wt[1] != 0.0 && $queryO ne "#and(" ) { $query .= " $wt[1] $queryO)"; }
    if( $wt[2] != 0.0 && $queryU ne "#and(" ) { $query .= " $wt[2] $queryU)"; }

    if( $query eq "#wand(" ) { return ""; } # return "" if we couldn't formulate anything

    return $query . " )";
}
