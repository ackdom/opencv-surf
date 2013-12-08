//
//  main.cpp
//  openCVTest
//
//  Created by Dominik Vesely on 11/8/13.
//  Copyright (c) 2013 Dominik Veselý. All rights reserved.
//

#include <stdio.h>
#include <iostream>
#include "opencv2/core/core.hpp"
#include "opencv2/features2d/features2d.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/calib3d/calib3d.hpp"
#include <opencv2/nonfree/features2d.hpp>

#include <hiredis/hiredis.h>



using namespace cv;

using namespace std;


vector<string> explode(const string& str, const char& ch) {
    string next;
    vector<string> result;
    
    // For each character in the string
    for (string::const_iterator it = str.begin(); it != str.end(); it++) {
        // If we've hit the terminal character
        if (*it == ch) {
            // If we have some characters accumulated
            if (!next.empty()) {
                // Add them to the result vector
                result.push_back(next);
                next.clear();
            }
        } else {
            // Accumulate the next character into the sequence
            next += *it;
        }
    }
    if (!next.empty())
        result.push_back(next);
    return result;
}
void readme();

/**
 * @function main
 * @brief Main function
 */
int main( int argc, char** argv )
{
    
    string fileName (argv[1]);
    vector<string> result = explode(fileName, '/');
	string name = result.back();
	vector<string> md5 = explode(name, '.');
    Mat img_1 = imread( argv[1], CV_LOAD_IMAGE_GRAYSCALE );
    
	if( !img_1.data )
	{ std::cout<< " --(!) Error reading image " << std::endl; return -1; }
    
    //-- Step 1: Detect the keypoints using SURF Detector
    int minHessian = 2500;
    
    SurfFeatureDetector detector( minHessian );
    
    std::vector<KeyPoint> keypoints_1;
    
    detector.detect( img_1, keypoints_1 );
    
    //-- Step 2: Calculate descriptors (feature vectors)
    SurfDescriptorExtractor extractor;
    
    Mat descriptors_1, descriptors_2;
    
    extractor.compute( img_1, keypoints_1, descriptors_1 );
    
    
    /*MatConstIterator_<float> it = descriptors_1.begin<float>(), it_end = descriptors_1.end<float>();
     for(; it != it_end; ++it)
     printf("aa %f\n", *it);*/
    //std::cout << descriptors_1.at<float>(0,0) << std::endl;
    //ss << descriptors_1;
    
  	redisContext *c;
	redisReply *reply;
	const char *hostname = "127.0.0.1";
	int port = 6379;
    
	struct timeval timeout = { 1, 500000 }; // 1.5 seconds
	c = redisConnectWithTimeout(hostname, port, timeout);
	if (c == NULL || c->err) {
		if (c) {
			printf("Connection error: %s\n", c->errstr);
			redisFree(c);
		} else {
			printf("Connection error: can't allocate redis context\n");
		}
		exit(1);
	}
    
    for(int i = 0; i < descriptors_1.rows; i++)
    {
        
        std::stringstream ss;
        for(int j = 0; j < descriptors_1.cols; j++)
        {
            ss << descriptors_1.at<float>(i,j) << " ";
        }
        const std::string& tmpStr = ss.str();
        /* Set a key */
		reply = (redisReply*)redisCommand(c,"SADD %s %s", md5[0].c_str(), tmpStr.c_str());
		freeReplyObject(reply);
    }    
    
    
    
    
    /* Disconnects and frees the context */
    redisFree(c);
    
   // waitKey(0);
    
    return 0;
}
