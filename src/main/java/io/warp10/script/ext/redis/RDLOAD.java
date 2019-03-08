//
//   Copyright 2019  SenX S.A.S.
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package io.warp10.script.ext.redis;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Charsets;

import io.warp10.script.NamedWarpScriptFunction;
import io.warp10.script.WarpScriptException;
import io.warp10.script.WarpScriptStack;
import io.warp10.script.WarpScriptStackFunction;
import redis.clients.jedis.Jedis;

public class RDLOAD extends NamedWarpScriptFunction implements WarpScriptStackFunction {
  public RDLOAD(String name) {
    super(name);
  }
  
  @Override
  public Object apply(WarpScriptStack stack) throws WarpScriptException {
    
    Object top = stack.pop();
    
    List<byte[]> keys = new ArrayList<byte[]>();
    
    boolean keylist = false;
    
    if (top instanceof String) {
      keys.add(top.toString().getBytes(Charsets.UTF_8));
    } else if (top instanceof List) {
      for (Object k: (List) top) {
        if (k instanceof String) {
          keys.add(k.toString().getBytes(Charsets.UTF_8));
        } else {
          throw new WarpScriptException(getName() + " expects a key (STRING) or a list thereof on top of the stack.");          
        }
      }
      keylist = true;
    } else { 
      throw new WarpScriptException(getName() + " expects a key (STRING) or a list thereof on top of the stack.");
    }
    
    Jedis jedis = null;
    
    try {
      top = stack.pop();
      URI uri = new URI(String.valueOf(top));
      jedis = new Jedis(uri);
      List<byte[]> values = jedis.mget(keys.toArray(new byte[0][]));
      
      if (keylist) {
        Map<String,byte[]> results = new HashMap<String,byte[]>();
        for (int i = 0; i < keys.size(); i++) {
          results.put(new String(keys.get(i), Charsets.UTF_8), values.get(i));
        }
        stack.push(results);
      } else {
        stack.push(values.get(0));
      }
    } catch (URISyntaxException use) {
      throw new WarpScriptException(getName() + " invalid URI.", use);
    } finally {
      if (null != jedis) {
        jedis.close();
      }
    }

    return stack;
  }
}
