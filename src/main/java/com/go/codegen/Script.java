/*
 Copyright (C) 2017 Ravinder Krishnaswamy

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice 
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH 
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND 
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, 
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS 
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER 
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF 
THIS SOFTWARE.
*/


package com.go.codegen;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class Script {
	
	private String mProgram;
	
	Script(UserContext ctxt, String code)
	{
		mProgram = code;
		
	}
	
	String getCode() 
	{ 
		return mProgram; 
	
	}

	void readFromFile(String fileName)
	{
		 try {
	            // FileReader reads text files in the default encoding.
	            FileReader fileReader = 
	                new FileReader(fileName);

	            // Always wrap FileReader in BufferedReader.
	            BufferedReader bufferedReader = 
	                new BufferedReader(fileReader);

	            String line;
	            String program = "";
	            while((line = bufferedReader.readLine()) != null) {
	                program += line + "\n";
	            }   

	            mProgram = program;
	            
	            // Always close files.
	            bufferedReader.close();     
	            
	        } catch(FileNotFoundException ex) {
	            System.out.println(
	                "Unable to open file '" + 
	                fileName + "'");                
	        } catch(IOException ex) {
	            System.out.println(
	                "Error reading file '" 
	                + fileName + "'");   
	        }
	}
	
	void writeToFile(String fileName) 
	{
		try {
			
			FileWriter fileWriter = new FileWriter(fileName);
			if (mProgram != null) {
				fileWriter.write(mProgram);
			}
			fileWriter.close();
			
		} catch (FileNotFoundException ex) {
			
            System.out.println(
                "Unable to open file '" + 
                fileName + "'");      
            
		} catch (IOException ex) {

            System.out.println(
                "Error reading file '" 
                + fileName + "'");   
		}
		
	}
	
}
