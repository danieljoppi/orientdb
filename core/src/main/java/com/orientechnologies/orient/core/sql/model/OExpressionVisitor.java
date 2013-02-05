/*
 * Copyright 2013 Orient Technologies.
 * Copyright 2013 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.sql.model;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public interface OExpressionVisitor {

  Object visit(OLiteral candidate, Object data);
  
  Object visit(OField candidate, Object data);
  
  Object visit(OContextVariable candidate, Object data);
  
  Object visit(OUnset candidate, Object data);
  
  Object visit(OFunction candidate, Object data);
  
  Object visit(OMethod candidate, Object data);
          
  Object visit(OOperator candidate, Object data);
  
  Object visit(OExpression candidate, Object data);
  
}